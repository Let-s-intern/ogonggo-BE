package com.ogonggo.userapi.concern.business

import com.ogonggo.core.concern.domain.ConcernComment
import com.ogonggo.core.concern.error.ConcernCommentErrorCode
import com.ogonggo.core.concern.implement.ConcernCommentAppender
import com.ogonggo.core.concern.implement.ConcernCommentLikeManager
import com.ogonggo.core.concern.implement.ConcernCommentLikeReader
import com.ogonggo.core.concern.implement.ConcernCommentReader
import com.ogonggo.core.concern.implement.ConcernCommentRemover
import com.ogonggo.core.concern.implement.ConcernMetricManager
import com.ogonggo.core.concern.implement.ConcernReader
import com.ogonggo.core.concern.implement.dto.ConcernCommentAppendDto
import com.ogonggo.core.concern.implement.dto.ConcernCommentPageDto
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserProfileDto
import com.ogonggo.userapi.user.implement.requireActive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 고민글의 부모 댓글은 화면의 답변, 대댓글은 답변에 단 답글이다. 답글에는 다시 답글을 달 수 없다.
 * 고민글의 `commentCount`는 남아 있는 답변 수이며 답글은 세지 않는다.
 */
@Service
class ConcernCommentService(
    private val userReader: UserReader,
    private val userProfileReader: UserProfileReader,
    private val concernReader: ConcernReader,
    private val concernMetricManager: ConcernMetricManager,
    private val commentReader: ConcernCommentReader,
    private val commentAppender: ConcernCommentAppender,
    private val commentRemover: ConcernCommentRemover,
    private val likeReader: ConcernCommentLikeReader,
    private val likeManager: ConcernCommentLikeManager,
    private val clock: Clock,
) {

    fun readComments(viewerUserId: Long?, concernId: Long, page: Int, size: Int): ConcernCommentPageResult {
        concernReader.read(concernId)
        val result = commentReader.readRootPage(concernId, page, size)
        val previews = commentReader.readReplyPreviews(concernId, result.comments.map { it.requiredId() }, REPLY_PREVIEW_SIZE)
        val context = readContext(viewerUserId, result.comments + previews.values.flatMap { it.comments })

        return ConcernCommentPageResult(
            items = result.comments.map { comment ->
                ConcernCommentRootResult(
                    comment = comment.toResult(context),
                    replies = previews[comment.requiredId()]?.toReplyPageResult(context)
                        ?: emptyReplyPage(),
                )
            },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    fun readReplies(
        viewerUserId: Long?,
        concernId: Long,
        parentId: Long,
        page: Int,
        size: Int,
    ): ConcernCommentReplyPageResult {
        concernReader.read(concernId)
        commentReader.readRoot(concernId, parentId)
        val result = commentReader.readReplyPage(concernId, parentId, page, size)
        return result.toReplyPageResult(readContext(viewerUserId, result.comments))
    }

    /** 작성할 때 관리자였으면 운영자 답변(`official`)으로 남긴다. */
    @Transactional
    fun create(userId: Long, concernId: Long, command: CreateConcernCommentCommand): Long {
        val account = userReader.read(userId)
        account.status.requireActive()
        concernReader.readForUpdate(concernId)
        command.parentId?.let { parentId ->
            if (commentReader.readParent(concernId, parentId).isReply()) {
                throw InvalidValueException(ConcernCommentErrorCode.CONCERN_COMMENT_NESTING_NOT_ALLOWED)
            }
        }

        val comment = commentAppender.append(
            ConcernCommentAppendDto(
                concernId = concernId,
                parentId = command.parentId,
                userId = userId,
                content = command.content,
                official = account.role == UserRole.ADMIN,
            ),
        )
        if (!comment.isReply()) {
            concernMetricManager.increaseCommentCount(concernId, LocalDateTime.now(clock))
        }
        return comment.requiredId()
    }

    @Transactional
    fun delete(userId: Long, concernId: Long, commentId: Long) {
        concernReader.readForUpdate(concernId)
        val comment = commentReader.readForUpdate(concernId, commentId)
        if (!comment.isWrittenBy(userId)) {
            throw ForbiddenException(ConcernCommentErrorCode.CONCERN_COMMENT_PERMISSION_DENIED)
        }

        val now = LocalDateTime.now(clock)
        commentRemover.remove(comment, now)
        if (!comment.isReply()) {
            concernMetricManager.decreaseCommentCount(concernId, now)
        }
    }

    @Transactional
    fun like(userId: Long, concernId: Long, commentId: Long) {
        userReader.read(userId).status.requireActive()
        concernReader.read(concernId)
        commentReader.read(concernId, commentId)
        likeManager.like(commentId, userId, LocalDateTime.now(clock))
    }

    @Transactional
    fun unlike(userId: Long, concernId: Long, commentId: Long) {
        concernReader.read(concernId)
        commentReader.read(concernId, commentId)
        likeManager.unlike(commentId, userId, LocalDateTime.now(clock))
    }

    private fun readContext(viewerUserId: Long?, comments: Collection<ConcernComment>): CommentViewContext {
        val commentIds = comments.map { it.requiredId() }
        return CommentViewContext(
            viewerUserId = viewerUserId,
            profiles = userProfileReader.readAll(comments.map { it.userId }.toSet()),
            likeCounts = likeReader.countAll(commentIds),
            likedCommentIds = viewerUserId
                ?.let { likeReader.readLikedCommentIds(it, commentIds) }
                ?: emptySet(),
        )
    }

    private fun ConcernComment.toResult(context: CommentViewContext): ConcernCommentResult {
        val commentId = requiredId()
        val deleted = deletedAt != null
        return ConcernCommentResult(
            id = commentId,
            parentId = parentId,
            author = context.profiles[userId].toAuthor(),
            official = official,
            content = if (deleted) DELETED_COMMENT_CONTENT else content,
            deleted = deleted,
            createdAt = createdAt,
            updatedAt = updatedAt,
            mine = context.viewerUserId == userId,
            likeCount = context.likeCounts[commentId] ?: 0L,
            liked = commentId in context.likedCommentIds,
        )
    }

    private fun ConcernCommentPageDto.toReplyPageResult(context: CommentViewContext) = ConcernCommentReplyPageResult(
        items = comments.map { it.toResult(context) },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

    private fun emptyReplyPage() = ConcernCommentReplyPageResult(
        items = emptyList(),
        page = 0,
        size = REPLY_PREVIEW_SIZE,
        totalElements = 0,
        totalPages = 0,
    )

    private fun ConcernComment.requiredId(): Long = checkNotNull(id) { "댓글 식별자가 없습니다." }

    private class CommentViewContext(
        val viewerUserId: Long?,
        val profiles: Map<Long, UserProfileDto>,
        val likeCounts: Map<Long, Long>,
        val likedCommentIds: Set<Long>,
    )

    companion object {
        private const val REPLY_PREVIEW_SIZE = 5
        private const val DELETED_COMMENT_CONTENT = "삭제된 댓글입니다"
    }
}
