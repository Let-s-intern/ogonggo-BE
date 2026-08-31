package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobMetric
import com.ogonggo.core.job.persistence.JobMetricJpaRepository
import org.springframework.stereotype.Component

interface JobMetricReader {
    fun read(jobId: Long): JobMetricData

    /** 목록 조회의 N+1을 피하기 위해 한 번에 조회하며, 지표 행이 없는 공고는 0으로 채운다. */
    fun readAll(jobIds: Collection<Long>): Map<Long, JobMetricData>
}

@Component
internal class JobMetricReaderImpl(
    private val jobMetricRepository: JobMetricJpaRepository,
) : JobMetricReader {

    override fun read(jobId: Long): JobMetricData =
        jobMetricRepository.findByJobId(jobId)?.let(JobMetricData::from) ?: JobMetricData.EMPTY

    override fun readAll(jobIds: Collection<Long>): Map<Long, JobMetricData> {
        if (jobIds.isEmpty()) {
            return emptyMap()
        }

        val metrics = jobMetricRepository.findAllByJobIdIn(jobIds.toSet())
            .associateBy(JobMetric::jobId)

        return jobIds.associateWith { jobId ->
            metrics[jobId]?.let(JobMetricData::from) ?: JobMetricData.EMPTY
        }
    }
}

data class JobMetricData(
    val viewCount: Long,
    val bookmarkCount: Long,
    val commentCount: Long,
) {
    companion object {
        val EMPTY = JobMetricData(viewCount = 0, bookmarkCount = 0, commentCount = 0)

        internal fun from(metric: JobMetric): JobMetricData = JobMetricData(
            viewCount = metric.viewCount,
            bookmarkCount = metric.bookmarkCount,
            commentCount = metric.commentCount,
        )
    }
}
