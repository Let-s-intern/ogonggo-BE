package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostComment
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostCommentJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecruitmentPostCommentRemover internal constructor(
    private val commentRepository: RecruitmentPostCommentJpaRepository,
) {

    fun remove(comment: RecruitmentPostComment, deletedAt: LocalDateTime): Int {
        checkNotNull(comment.id) { "삭제할 댓글 식별자가 없습니다." }
        comment.delete(deletedAt)
        commentRepository.save(comment)
        return 1
    }
}
