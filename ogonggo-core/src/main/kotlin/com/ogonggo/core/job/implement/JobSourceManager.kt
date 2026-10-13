package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobSource
import com.ogonggo.core.job.persistence.JobSourceJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class JobSourceManager internal constructor(
    private val jobSourceRepository: JobSourceJpaRepository,
) {

    /** 공고의 원문으로 저장한다. 이미 있으면 바꾼다. 공고가 있는지는 호출자가 먼저 확인한다. */
    @Transactional
    fun save(jobId: Long, html: String) {
        val existing = jobSourceRepository.findByJobId(jobId)
        if (existing == null) {
            jobSourceRepository.save(JobSource(jobId = jobId, html = html))
            return
        }
        existing.replace(html)
    }
}
