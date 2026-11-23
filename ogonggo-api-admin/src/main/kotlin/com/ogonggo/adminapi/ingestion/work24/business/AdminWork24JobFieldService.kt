package com.ogonggo.adminapi.ingestion.work24.business

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.work24jobfield.implement.Work24JobFieldManager
import com.ogonggo.core.work24jobfield.implement.Work24JobFieldReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 운영자가 고용24 채용정보 수집에서 직군을 켜고 끈다. 바꾼 값은 다음 수집 회차부터 쓴다.
 * 이미 등록한 공고는 그대로 둔다.
 */
@Service
class AdminWork24JobFieldService(
    private val work24JobFieldReader: Work24JobFieldReader,
    private val work24JobFieldManager: Work24JobFieldManager,
) {

    fun findAll(): Map<JobField, Boolean> = work24JobFieldReader.findAll()

    @Transactional
    fun replace(jobField: JobField, enabled: Boolean) {
        work24JobFieldManager.replace(jobField, enabled)
    }
}
