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
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserProfileDto
import com.ogonggo.userapi.user.implement.requireActive
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
        val profiles = readProfiles(result.comments + previews.values.flatMap { it.comments })

        return RecruitmentPostCommentPageResult(
            items = result.comments.map { comment ->
                RecruitmentPostCommentRootResult(
                    comment = comment.toResult(userId, profiles),
                    replies = previews[comment.requiredId()]?.toReplyPageResult(userId, profiles)
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
        val profiles = readProfiles(result.comments)

        return RecruitmentPostCommentReplyPageResult(
            items = result.comments.map { it.toResult(userId, profiles) },
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
        postReader.readPublishedForUpdate(postId)
        command.parentId?.let { parentId -> readValidParent(parentId, postId) }

        val comment = commentAppender.append(
            RecruitmentPostCommentAppendDto(
                postId = postId,
                parentId = command.parentId,
                userId = userId,
                content = command.content,
            ),
        )
        recruitmentPostMetricManager.increaseCommentCount(postId, LocalDateTime.now(clock))
        return checkNotNull(comment.id) { "저장된 댓글 식별자가 없습니다." }
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

    private fun readProfiles(comments: Collection<RecruitmentPostComment>): Map<Long, UserProfileDto> =
        userProfileReader.readAll(comments.map { it.userId }.toSet())

    private fun RecruitmentPostComment.toResult(
        viewerUserId: Long?,
        profiles: Map<Long, UserProfileDto>,
    ): RecruitmentPostCommentResult {
        val profile = profiles[userId]
        return RecruitmentPostCommentResult(
            id = requiredId(),
            parentId = parentId,
            author = RecruitmentPostCommentAuthorResult(
                userId = userId,
                nickname = profile?.nickname,
                profileImageUrl = profile?.profileImageUrl,
            ),
            content = if (deletedAt == null) content else DELETED_COMMENT_CONTENT,
            createdAt = createdAt,
            updatedAt = updatedAt,
            mine = viewerUserId == userId,
        )
    }

    private fun RecruitmentPostCommentPageDto.toReplyPageResult(
        viewerUserId: Long?,
        profiles: Map<Long, UserProfileDto>,
    ): RecruitmentPostCommentReplyPageResult = RecruitmentPostCommentReplyPageResult(
        items = comments.map { it.toResult(viewerUserId, profiles) },
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
)
