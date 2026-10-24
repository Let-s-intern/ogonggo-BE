package com.ogonggo.userapi.recruitmentpost.business

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostComment
import com.ogonggo.core.recruitmentpost.error.RecruitmentPostCommentErrorCode
import com.ogonggo.core.recruitmentpost.error.RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_PERMISSION_DENIED
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostCommentAppender
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostCommentReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostCommentRemover
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostCommentReportAppender
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricManager
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostReader
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostCommentAppendDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostCommentPageDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostCommentReportAppendDto
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.user.implement.requireActive
import org.springframework.context.ApplicationEventPublisher
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
    private val postReader: RecruitmentPostReader,
    private val commentReader: RecruitmentPostCommentReader,
    private val commentAppender: RecruitmentPostCommentAppender,
    private val commentRemover: RecruitmentPostCommentRemover,
    private val userProfileReader: UserProfileReader,
    private val recruitmentPostMetricManager: RecruitmentPostMetricManager,
    private val reportAppender: RecruitmentPostCommentReportAppender,
    private val clock: Clock,
    private val eventPublisher: ApplicationEventPublisher,
) {

    fun readComments(
        userId: Long?,
        postId: Long,
        page: Int,
        size: Int,
    ): RecruitmentPostCommentPageResult {
        postReader.readPublished(postId)
        val result = commentReader.readRootPage(postId, page, size)
        val parentIds = result.comments.mapNotNull { it.id }
        val previews = commentReader.readReplyPreviews(postId, parentIds, REPLY_PREVIEW_SIZE)
        val authors = readAuthors(result.comments + previews.values.flatMap { it.comments })

        return RecruitmentPostCommentPageResult(
            items = result.comments.map { comment ->
                RecruitmentPostCommentRootResult(
                    comment = comment.toResult(userId, authors),
                    replies = previews[comment.requiredId()]?.toReplyPageResult(userId, authors)
                        ?: RecruitmentPostCommentReplyPageResult.EMPTY,
                )
            },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    fun readReplies(
        userId: Long?,
        postId: Long,
        parentId: Long,
        page: Int,
        size: Int,
    ): RecruitmentPostCommentReplyPageResult {
        postReader.readPublished(postId)
        commentReader.readRoot(postId, parentId)
        val result = commentReader.readReplyPage(postId, parentId, page, size)
        val authors = readAuthors(result.comments)

        return RecruitmentPostCommentReplyPageResult(
            items = result.comments.map { it.toResult(userId, authors) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional
    fun delete(userId: Long, postId: Long, commentId: Long) {
        verifyActiveUser(userId)
        postReader.readPublishedForUpdate(postId)

        val comment = commentReader.readInPostForUpdate(postId, commentId)
        if (comment.userId != userId) {
            throw ForbiddenException(RECRUITMENT_POST_COMMENT_PERMISSION_DENIED)
        }

        val now = LocalDateTime.now(clock)
        val deletedCount = commentRemover.remove(comment, now)
        recruitmentPostMetricManager.decreaseCommentCount(postId, deletedCount, now)
    }

    @Transactional
    fun create(userId: Long, postId: Long, command: CreateRecruitmentPostCommentCommand): Long {
        verifyActiveUser(userId)
        val post = postReader.readPublishedForUpdate(postId)
        val parent = command.parentId?.let { parentId -> readValidParent(parentId, postId) }

        val comment = commentAppender.append(
            RecruitmentPostCommentAppendDto(
                postId = postId,
                parentId = command.parentId,
                userId = userId,
                content = command.content,
            ),
        )
        val now = LocalDateTime.now(clock)
        recruitmentPostMetricManager.increaseCommentCount(postId, now)
        val commentId = checkNotNull(comment.id) { "저장된 댓글 식별자가 없습니다." }
        eventPublisher.publishEvent(
            RecruitmentPostCommentCreatedEvent(
                postId = postId,
                postTitle = post.title,
                commentId = commentId,
                parentId = command.parentId,
                parentAuthorUserId = parent?.userId,
                authorUserId = userId,
                content = command.content,
                occurredAt = now,
            ),
        )
        return commentId
    }

    @Transactional
    fun report(userId: Long, postId: Long, commentId: Long, reason: String?) {
        verifyActiveUser(userId)
        postReader.readPublished(postId)
        val comment = commentReader.readInPost(postId, commentId)
        reportAppender.append(
            RecruitmentPostCommentReportAppendDto(
                commentId = checkNotNull(comment.id) { "신고할 댓글 식별자가 없습니다." },
                userId = userId,
                reason = reason,
            ),
        )
    }

    private fun readValidParent(
        parentId: Long,
        postId: Long,
    ) = commentReader.readForUpdate(parentId).also { parent ->
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
        userReader.read(userId).status.requireActive()
    }

    /** 작성자 역할은 지금 역할이다. 관리자가 역할을 바꾸면 이미 쓴 댓글의 표시도 함께 바뀐다. */
    private fun readAuthors(comments: Collection<RecruitmentPostComment>): Map<Long, RecruitmentPostCommentAuthorResult> {
        val userIds = comments.map { it.userId }.toSet()
        val profiles = userProfileReader.readAll(userIds)
        val roles = userReader.readRoles(userIds)
        return userIds.associateWith { userId ->
            val profile = profiles[userId]
            RecruitmentPostCommentAuthorResult(
                userId = userId,
                nickname = profile?.nickname,
                profileImageUrl = profile?.profileImageUrl,
                // 회원 행은 지우지 않으므로 비는 일은 없다. 비더라도 표시 없이 보이도록 일반 회원으로 둔다.
                role = roles[userId] ?: UserRole.USER,
            )
        }
    }

    private fun RecruitmentPostComment.toResult(
        viewerUserId: Long?,
        authors: Map<Long, RecruitmentPostCommentAuthorResult>,
    ): RecruitmentPostCommentResult {
        return RecruitmentPostCommentResult(
            id = requiredId(),
            parentId = parentId,
            author = authors.getValue(userId),
            content = if (deletedAt == null) content else DELETED_COMMENT_CONTENT,
            createdAt = createdAt,
            updatedAt = updatedAt,
            mine = viewerUserId == userId,
        )
    }

    private fun RecruitmentPostCommentPageDto.toReplyPageResult(
        viewerUserId: Long?,
        authors: Map<Long, RecruitmentPostCommentAuthorResult>,
    ): RecruitmentPostCommentReplyPageResult = RecruitmentPostCommentReplyPageResult(
        items = comments.map { it.toResult(viewerUserId, authors) },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

    private fun RecruitmentPostComment.requiredId(): Long =
        checkNotNull(id) { "댓글 식별자가 없습니다." }

    companion object {
        private const val REPLY_PREVIEW_SIZE = 5
        private const val DELETED_COMMENT_CONTENT = "삭제된 댓글입니다"
    }
}

data class RecruitmentPostCommentPageResult(
    val items: List<RecruitmentPostCommentRootResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class RecruitmentPostCommentReplyPageResult(
    val items: List<RecruitmentPostCommentResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        val EMPTY = RecruitmentPostCommentReplyPageResult(
            items = emptyList(),
            page = 0,
            size = 5,
            totalElements = 0,
            totalPages = 0,
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
    val role: UserRole,
)
