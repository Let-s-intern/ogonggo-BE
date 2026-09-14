package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.error.RecruitmentPostCommentErrorCode
import com.ogonggo.core.community.domain.RecruitmentPostComment
import com.ogonggo.core.community.implement.PostReader
import com.ogonggo.core.community.implement.PostMetricManager
import com.ogonggo.core.community.implement.RecruitmentPostCommentAppender
import com.ogonggo.core.community.implement.RecruitmentPostCommentAppendCommand
import com.ogonggo.core.community.implement.RecruitmentPostCommentCursor
import com.ogonggo.core.community.implement.RecruitmentPostCommentPage
import com.ogonggo.core.community.implement.RecruitmentPostCommentReader
import com.ogonggo.core.community.implement.RecruitmentPostCommentRemover
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.community.error.RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_PERMISSION_DENIED
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.dto.UserProfileDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

data class CreateRecruitmentPostCommentCommand(
    val parentId: Long?,
    val content: String,
)

@Service
class RecruitmentPostCommentService(
    private val userReader: UserReader,
    private val postReader: PostReader,
    private val commentReader: RecruitmentPostCommentReader,
    private val commentAppender: RecruitmentPostCommentAppender,
    private val commentRemover: RecruitmentPostCommentRemover,
    private val userProfileReader: UserProfileReader,
    private val postMetricManager: PostMetricManager,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun readComments(
        userId: Long?,
        postId: Long,
        cursor: RecruitmentPostCommentCursor?,
        size: Int,
    ): RecruitmentPostCommentPageResult {
        postReader.readPublished(postId)
        val page = commentReader.readRootPage(postId, cursor, size)
        val parentIds = page.comments.mapNotNull { it.id }
        val previews = commentReader.readReplyPreviews(postId, parentIds, REPLY_PREVIEW_SIZE)
        val profiles = readProfiles(page.comments + previews.values.flatMap { it.comments })

        return RecruitmentPostCommentPageResult(
            items = page.comments.map { comment ->
                RecruitmentPostCommentRootResult(
                    comment = comment.toResult(userId, profiles),
                    replies = previews[comment.requiredId()]?.toReplyPageResult(userId, profiles)
                        ?: RecruitmentPostCommentReplyPageResult.EMPTY,
                )
            },
            nextCursor = page.nextCursor,
            hasNext = page.hasNext,
        )
    }

    @Transactional(readOnly = true)
    fun readReplies(
        userId: Long?,
        postId: Long,
        parentId: Long,
        cursor: RecruitmentPostCommentCursor?,
        size: Int,
    ): RecruitmentPostCommentReplyPageResult {
        postReader.readPublished(postId)
        commentReader.readRoot(postId, parentId)
        val page = commentReader.readReplyPage(postId, parentId, cursor, size)
        val profiles = readProfiles(page.comments)

        return RecruitmentPostCommentReplyPageResult(
            items = page.comments.map { it.toResult(userId, profiles) },
            nextCursor = page.nextCursor,
            hasNext = page.hasNext,
        )
    }

    @Transactional
    fun delete(userId: Long, postId: Long, commentId: Long) {
        verifyActiveUser(userId)
        postReader.readPublished(postId)

        val comment = commentReader.readInPost(postId, commentId)
        if (comment.userId != userId) {
            throw ForbiddenException(RECRUITMENT_POST_COMMENT_PERMISSION_DENIED)
        }

        val removedCount = commentRemover.remove(comment)
        postMetricManager.decreaseCommentCount(postId, removedCount, LocalDateTime.now(clock))
    }

    @Transactional
    fun create(userId: Long, postId: Long, command: CreateRecruitmentPostCommentCommand): Long {
        verifyActiveUser(userId)
        val post = postReader.readPublished(postId)
        val parent = command.parentId?.let { parentId -> readValidParent(parentId, postId) }

        val comment = commentAppender.append(
            RecruitmentPostCommentAppendCommand(
                post = post,
                parent = parent,
                userId = userId,
                content = command.content,
            ),
        )
        postMetricManager.increaseCommentCount(postId, LocalDateTime.now(clock))
        return checkNotNull(comment.id) { "저장된 댓글 식별자가 없습니다." }
    }

    private fun readValidParent(
        parentId: Long,
        postId: Long,
    ) = commentReader.read(parentId).also { parent ->
        if (!parent.belongsTo(postId)) {
            throw InvalidValueException(
                RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_PARENT_TARGET_MISMATCH,
            )
        }
        if (parent.isReply()) {
            throw InvalidValueException(
                RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_NESTING_NOT_ALLOWED,
            )
        }
    }

    private fun verifyActiveUser(userId: Long) {
        when (userReader.read(userId).status) {
            UserStatus.ACTIVE -> Unit
            UserStatus.SUSPENDED -> throw ForbiddenException(UserErrorCode.USER_SUSPENDED)
            UserStatus.WITHDRAWN -> throw ForbiddenException(UserErrorCode.USER_WITHDRAWN)
        }
    }

    private fun readProfiles(comments: Collection<RecruitmentPostComment>): Map<Long, UserProfileDto> =
        userProfileReader.readAll(comments.map { it.userId }.toSet())

    private fun RecruitmentPostComment.toResult(
        viewerUserId: Long?,
        profiles: Map<Long, UserProfileDto>,
    ): RecruitmentPostCommentResult {
        val profile = profiles[userId]
        return RecruitmentPostCommentResult(
            id = requiredId(),
            parentId = parent?.id,
            author = RecruitmentPostCommentAuthorResult(
                userId = userId,
                nickname = profile?.nickname,
                profileImageUrl = profile?.profileImageUrl,
            ),
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
            mine = viewerUserId == userId,
        )
    }

    private fun RecruitmentPostCommentPage.toReplyPageResult(
        viewerUserId: Long?,
        profiles: Map<Long, UserProfileDto>,
    ): RecruitmentPostCommentReplyPageResult = RecruitmentPostCommentReplyPageResult(
        items = comments.map { it.toResult(viewerUserId, profiles) },
        nextCursor = nextCursor,
        hasNext = hasNext,
    )

    private fun RecruitmentPostComment.requiredId(): Long =
        checkNotNull(id) { "댓글 식별자가 없습니다." }

    companion object {
        private const val REPLY_PREVIEW_SIZE = 5
    }
}

data class RecruitmentPostCommentPageResult(
    val items: List<RecruitmentPostCommentRootResult>,
    val nextCursor: RecruitmentPostCommentCursor?,
    val hasNext: Boolean,
)

data class RecruitmentPostCommentReplyPageResult(
    val items: List<RecruitmentPostCommentResult>,
    val nextCursor: RecruitmentPostCommentCursor?,
    val hasNext: Boolean,
) {
    companion object {
        val EMPTY = RecruitmentPostCommentReplyPageResult(
            items = emptyList(),
            nextCursor = null,
            hasNext = false,
        )
    }
}

data class RecruitmentPostCommentRootResult(
    val comment: RecruitmentPostCommentResult,
    val replies: RecruitmentPostCommentReplyPageResult,
)

data class RecruitmentPostCommentResult(
    val id: Long,
    val parentId: Long?,
    val author: RecruitmentPostCommentAuthorResult,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val mine: Boolean,
)

data class RecruitmentPostCommentAuthorResult(
    val userId: Long,
    val nickname: String?,
    val profileImageUrl: String?,
)
