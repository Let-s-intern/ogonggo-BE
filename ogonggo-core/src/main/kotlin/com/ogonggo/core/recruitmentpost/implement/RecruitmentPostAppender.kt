package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostAppendDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostDraftAppendDto
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostJpaRepository
import org.springframework.stereotype.Component

@Component
class RecruitmentPostAppender internal constructor(
    private val postRepository: RecruitmentPostJpaRepository,
) {

    fun append(command: RecruitmentPostAppendDto): RecruitmentPost = postRepository.save(command.toEntity())

    fun appendDraft(command: RecruitmentPostDraftAppendDto): RecruitmentPost = postRepository.save(command.toEntity())
}
