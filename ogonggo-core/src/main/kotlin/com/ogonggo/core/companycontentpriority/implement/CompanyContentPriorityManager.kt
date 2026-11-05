package com.ogonggo.core.companycontentpriority.implement

import com.ogonggo.core.companycontentpriority.domain.CompanyContentPriority
import com.ogonggo.core.companycontentpriority.persistence.CompanyContentPriorityJpaRepository
import org.springframework.stereotype.Component

@Component
class CompanyContentPriorityManager internal constructor(
    private val priorityRepository: CompanyContentPriorityJpaRepository,
) {

    /** 켜짐 여부를 바꾼다. 행이 없으면 고정 식별자로 만든다. */
    fun replace(enabled: Boolean) {
        val priority = priorityRepository.findById(CompanyContentPriority.SINGLETON_ID).orElse(null)
        if (priority == null) {
            priorityRepository.save(CompanyContentPriority(enabled))
            return
        }
        priority.replace(enabled)
    }
}
