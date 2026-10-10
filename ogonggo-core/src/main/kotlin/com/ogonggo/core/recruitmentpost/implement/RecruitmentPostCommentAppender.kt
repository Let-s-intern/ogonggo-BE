package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostComment
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostCommentJpaRepository
import org.springframework.stereotype.Component
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostCommentAppendDto

@Component
class RecruitmentPostCommentAppender internal constructor(
    private val commentRepository: RecruitmentPostCommentJpaRepository,
) {

    fun append(command: RecruitmentPostCommentAppendDto): RecruitmentPostComment =
        commentRepository.save(command.toEntity())
}
