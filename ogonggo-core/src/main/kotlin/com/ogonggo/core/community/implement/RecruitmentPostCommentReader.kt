package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostComment
import com.ogonggo.core.community.error.RecruitmentPostCommentErrorCode
import com.ogonggo.core.community.persistence.RecruitmentPostCommentJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

interface RecruitmentPostCommentReader {
    fun read(commentId: Long): RecruitmentPostComment
}

@Component
internal class RecruitmentPostCommentReaderImpl(
    private val commentRepository: RecruitmentPostCommentJpaRepository,
) : RecruitmentPostCommentReader {

    override fun read(commentId: Long): RecruitmentPostComment =
        commentRepository.findByIdOrNull(commentId)
            ?: throw EntityNotFoundException(
                RecruitmentPostCommentErrorCode.RECRUITMENT_POST_COMMENT_PARENT_NOT_FOUND,
            )
}
