package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.implement.dto.RecruitmentPostAppendDto
import com.ogonggo.core.community.implement.dto.RecruitmentPostDraftAppendDto
import com.ogonggo.core.community.persistence.RecruitmentPostJpaRepository
import org.springframework.stereotype.Component

@Component
class RecruitmentPostAppender internal constructor(
    private val postRepository: RecruitmentPostJpaRepository,
) {

    fun append(command: RecruitmentPostAppendDto): RecruitmentPost = postRepository.save(command.toEntity())

    fun appendDraft(command: RecruitmentPostDraftAppendDto): RecruitmentPost = postRepository.save(command.toEntity())
}
