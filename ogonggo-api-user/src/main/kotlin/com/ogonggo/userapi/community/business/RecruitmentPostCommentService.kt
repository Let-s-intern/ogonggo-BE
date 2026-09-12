package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.error.RecruitmentPostCommentErrorCode
import com.ogonggo.core.community.implement.PostReader
import com.ogonggo.core.community.implement.RecruitmentPostCommentAppender
import com.ogonggo.core.community.implement.RecruitmentPostCommentAppendCommand
import com.ogonggo.core.community.implement.RecruitmentPostCommentReader
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
) {

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
        if (parent.isDeleted()) {
            throw ConflictException(
                RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_PARENT_DELETED,
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
}
