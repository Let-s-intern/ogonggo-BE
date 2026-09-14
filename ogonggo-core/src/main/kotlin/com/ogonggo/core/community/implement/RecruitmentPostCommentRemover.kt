package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostComment
import com.ogonggo.core.community.persistence.RecruitmentPostCommentJpaRepository
import org.springframework.stereotype.Component

interface RecruitmentPostCommentRemover {
    fun remove(comment: RecruitmentPostComment): Int
}

@Component
internal class RecruitmentPostCommentRemoverImpl(
    private val commentRepository: RecruitmentPostCommentJpaRepository,
) : RecruitmentPostCommentRemover {

    override fun remove(comment: RecruitmentPostComment): Int {
        val commentId = checkNotNull(comment.id) { "삭제할 댓글 식별자가 없습니다." }

        // self FK 때문에 부모를 삭제하기 전에 직속 대댓글을 먼저 삭제한다.
        val replyCount = commentRepository.deleteAllByParentId(commentId)
        commentRepository.delete(comment)
        return replyCount + 1
    }
}
