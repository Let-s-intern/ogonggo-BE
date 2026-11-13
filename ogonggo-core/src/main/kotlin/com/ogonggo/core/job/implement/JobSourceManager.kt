package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobSource
import com.ogonggo.core.job.persistence.JobSourceJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class JobSourceManager internal constructor(
    private val jobSourceRepository: JobSourceJpaRepository,
) {

    /** 공고의 원문으로 저장한다. 이미 있으면 바꾸고, 삭제된 원문이면 되살린다. 공고가 있는지는 호출자가 먼저 확인한다. */
    @Transactional
    fun save(jobId: Long, html: String) {
        val existing = jobSourceRepository.findByJobId(jobId)
        if (existing == null) {
            jobSourceRepository.save(JobSource(jobId = jobId, html = html))
            return
        }
        existing.replace(html)
    }

    /** 공고의 원문을 소프트 삭제한다. 원문이 없으면 아무것도 하지 않는다. */
    @Transactional
    fun delete(jobId: Long, now: LocalDateTime) {
        jobSourceRepository.findByJobId(jobId)?.delete(now)
    }
}
