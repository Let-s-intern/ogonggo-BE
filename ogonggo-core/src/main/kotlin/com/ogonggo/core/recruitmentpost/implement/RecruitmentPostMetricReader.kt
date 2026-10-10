package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostMetric
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostMetricJpaRepository
import org.springframework.stereotype.Component
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostMetricDto

@Component
class RecruitmentPostMetricReader internal constructor(
    private val recruitmentPostMetricRepository: RecruitmentPostMetricJpaRepository,
) {

    fun read(postId: Long): RecruitmentPostMetricDto =
        recruitmentPostMetricRepository.findByPostId(postId)?.let(RecruitmentPostMetricDto::from) ?: RecruitmentPostMetricDto.EMPTY

    fun readAll(postIds: Collection<Long>): Map<Long, RecruitmentPostMetricDto> {
        if (postIds.isEmpty()) return emptyMap()

        val metrics = recruitmentPostMetricRepository.findAllByPostIdIn(postIds.toSet())
            .associateBy(RecruitmentPostMetric::postId)

        return postIds.associateWith { postId ->
            metrics[postId]?.let(RecruitmentPostMetricDto::from) ?: RecruitmentPostMetricDto.EMPTY
        }
    }
}
