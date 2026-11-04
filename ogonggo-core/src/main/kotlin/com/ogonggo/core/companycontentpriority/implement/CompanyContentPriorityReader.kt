package com.ogonggo.core.companycontentpriority.implement

import com.ogonggo.core.companycontentpriority.domain.CompanyContentPriority
import com.ogonggo.core.companycontentpriority.persistence.CompanyContentPriorityJpaRepository
import org.springframework.stereotype.Component

@Component
class CompanyContentPriorityReader internal constructor(
    private val priorityRepository: CompanyContentPriorityJpaRepository,
) {

    /** 저장한 값이다. 운영자가 한 번도 바꾸지 않았으면 켜짐이다. */
    fun isEnabled(): Boolean =
        priorityRepository.findFirstByOrderByIdAsc()?.enabled ?: CompanyContentPriority.DEFAULT_ENABLED
}
