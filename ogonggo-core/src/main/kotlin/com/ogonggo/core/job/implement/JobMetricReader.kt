package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobMetric
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.core.job.persistence.JobMetricJpaRepository
import org.springframework.stereotype.Component

@Component
class JobMetricReader internal constructor(
    private val jobMetricRepository: JobMetricJpaRepository,
) {

    fun read(jobId: Long): JobMetricDto =
        jobMetricRepository.findByJobId(jobId)?.let(JobMetricDto::from) ?: JobMetricDto.EMPTY

    /** 목록 조회의 N+1을 피하기 위해 한 번에 조회하며, 지표 행이 없는 공고는 0으로 채운다. */

    fun readAll(jobIds: Collection<Long>): Map<Long, JobMetricDto> {
        if (jobIds.isEmpty()) {
            return emptyMap()
        }

        val metrics = jobMetricRepository.findAllByJobIdIn(jobIds.toSet())
            .associateBy(JobMetric::jobId)

        return jobIds.associateWith { jobId ->
            metrics[jobId]?.let(JobMetricDto::from) ?: JobMetricDto.EMPTY
        }
    }
}
