package com.ogonggo.core.job.implement

import com.ogonggo.core.job.persistence.JobSourceJpaRepository
import org.springframework.stereotype.Component

@Component
class JobSourceReader internal constructor(
    private val jobSourceRepository: JobSourceJpaRepository,
) {

    /** 공고의 원문 HTML이다. 크롤러가 원문을 보내지 않은 공고, 원문 이미지를 올리지 않았거나 모두 뺀 기업회원 공고는 `null`이다. */
    fun readHtml(jobId: Long): String? = jobSourceRepository.findByJobId(jobId)?.takeIf { it.deletedAt == null }?.html
}
