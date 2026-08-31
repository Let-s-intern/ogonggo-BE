package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobMetric
import com.ogonggo.core.job.persistence.JobBookmarkJpaRepository
import com.ogonggo.core.job.persistence.JobMetricJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface JobMetricManager {
    fun increaseViewCount(jobId: Long, now: LocalDateTime)

    /**
     * 북마크 수를 증감하지 않고 활성 북마크를 다시 세어 맞춘다.
     * 몇 번을 실행해도 결과가 같으므로 갱신을 한 번 놓쳐도 다음 갱신에서 값이 스스로 복구된다.
     */
    fun syncBookmarkCount(jobId: Long)
}

@Component
internal class JobMetricManagerImpl(
    private val jobMetricRepository: JobMetricJpaRepository,
    private val jobBookmarkRepository: JobBookmarkJpaRepository,
) : JobMetricManager {

    /**
     * 지표 행은 공고 생성이 아니라 첫 지표 발생 시점에 만들어진다.
     * 같은 공고를 동시에 조회하면 두 요청이 모두 행을 만들려 할 수 있으므로
     * 유니크 제약 위반을 확인한 뒤 증가 UPDATE로 되돌아간다.
     */
    override fun increaseViewCount(jobId: Long, now: LocalDateTime) {
        if (jobMetricRepository.increaseViewCount(jobId, now) > 0) {
            return
        }

        try {
            jobMetricRepository.saveAndFlush(JobMetric(jobId = jobId, viewCount = 1))
        } catch (exception: DataIntegrityViolationException) {
            jobMetricRepository.increaseViewCount(jobId, now)
        }
    }

    override fun syncBookmarkCount(jobId: Long) {
        val metric = jobMetricRepository.findByJobId(jobId) ?: JobMetric(jobId = jobId)
        metric.updateBookmarkCount(jobBookmarkRepository.countByJobIdAndDeletedAtIsNull(jobId))
        jobMetricRepository.save(metric)
    }
}
