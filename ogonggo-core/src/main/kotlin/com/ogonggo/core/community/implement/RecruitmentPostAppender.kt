package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.persistence.RecruitmentPostJpaRepository
import org.springframework.stereotype.Component

@Component
class RecruitmentPostAppender internal constructor(
    private val postRepository: RecruitmentPostJpaRepository,
) {

    fun append(command: RecruitmentPostAppendCommand): RecruitmentPost = postRepository.save(command.toEntity())
}
