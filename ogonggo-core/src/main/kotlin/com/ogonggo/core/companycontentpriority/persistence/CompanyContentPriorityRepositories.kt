package com.ogonggo.core.companycontentpriority.persistence

import com.ogonggo.core.companycontentpriority.domain.CompanyContentPriority
import org.springframework.data.jpa.repository.JpaRepository

internal interface CompanyContentPriorityJpaRepository : JpaRepository<CompanyContentPriority, Long>
