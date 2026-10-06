package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostComment
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostCommentJpaRepository
import org.springframework.stereotype.Component

@Component
class RecruitmentPostCommentAppender internal constructor(
    private val commentRepository: RecruitmentPostCommentJpaRepository,
) {

    fun append(command: RecruitmentPostCommentAppendCommand): RecruitmentPostComment =
        commentRepository.save(command.toEntity())
}
