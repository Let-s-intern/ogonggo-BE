package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostComment
import com.ogonggo.core.community.persistence.RecruitmentPostCommentJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecruitmentPostCommentRemover internal constructor(
    private val commentRepository: RecruitmentPostCommentJpaRepository,
) {

    fun remove(comment: RecruitmentPostComment, deletedAt: LocalDateTime): Int {
        val commentId = checkNotNull(comment.id) { "삭제할 댓글 식별자가 없습니다." }

        val replyCount = commentRepository.softDeleteActiveReplies(commentId, deletedAt)
        comment.delete(deletedAt)
        commentRepository.save(comment)
        return replyCount + 1
    }
}
