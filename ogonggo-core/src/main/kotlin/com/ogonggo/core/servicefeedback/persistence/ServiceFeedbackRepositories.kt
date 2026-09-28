package com.ogonggo.core.servicefeedback.persistence

import com.ogonggo.core.servicefeedback.domain.ServiceFeedback
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

internal interface ServiceFeedbackJpaRepository : JpaRepository<ServiceFeedback, Long> {
    fun findAllByOrderByIdDesc(pageable: Pageable): Page<ServiceFeedback>
}
