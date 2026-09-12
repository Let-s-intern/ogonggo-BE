package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostComment
import com.ogonggo.core.community.persistence.RecruitmentPostCommentJpaRepository
import org.springframework.stereotype.Component

interface RecruitmentPostCommentAppender {
    fun append(command: RecruitmentPostCommentAppendCommand): RecruitmentPostComment
}

@Component
internal class RecruitmentPostCommentAppenderImpl(
    private val commentRepository: RecruitmentPostCommentJpaRepository,
) : RecruitmentPostCommentAppender {

    override fun append(command: RecruitmentPostCommentAppendCommand): RecruitmentPostComment =
        commentRepository.save(command.toEntity())
}
