package com.ogonggo.adminapi.companycontentpriority.business

import com.ogonggo.core.companycontentpriority.implement.CompanyContentPriorityManager
import com.ogonggo.core.companycontentpriority.implement.CompanyContentPriorityReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 운영자가 기업회원 공고·부트캠프 우선 노출을 켜고 끈다. 바꾼 값은 다음 공개 목록 요청부터 쓴다. */
@Service
class AdminCompanyContentPriorityService(
    private val priorityReader: CompanyContentPriorityReader,
    private val priorityManager: CompanyContentPriorityManager,
) {

    fun isEnabled(): Boolean = priorityReader.isEnabled()

    @Transactional
    fun replace(enabled: Boolean) {
        priorityManager.replace(enabled)
    }
}
