package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobSourceUrlClick
import com.ogonggo.core.job.persistence.JobSourceUrlClickJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

interface JobSourceUrlClickAppender {
    /** 같은 사용자가 같은 공고를 여러 번 눌러도 최초 기록만 남기고 조용히 넘어간다. */
    fun append(userId: Long, jobId: Long)
}

@Component
internal class JobSourceUrlClickAppenderImpl(
    private val jobSourceUrlClickRepository: JobSourceUrlClickJpaRepository,
) : JobSourceUrlClickAppender {

    override fun append(userId: Long, jobId: Long) {
        if (jobSourceUrlClickRepository.existsByJobIdAndUserId(jobId, userId)) {
            return
        }

        try {
            jobSourceUrlClickRepository.saveAndFlush(JobSourceUrlClick(jobId = jobId, userId = userId))
        } catch (exception: DataIntegrityViolationException) {
            // 같은 사용자가 버튼을 연속으로 눌러 동시에 저장되면 유니크 제약이 막는다. 기록은 이미 남았으므로 성공으로 본다.
        }
    }
}
