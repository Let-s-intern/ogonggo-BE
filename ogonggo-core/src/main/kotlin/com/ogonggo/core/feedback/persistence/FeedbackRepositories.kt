package com.ogonggo.core.feedback.persistence

import com.ogonggo.core.feedback.domain.Feedback
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

internal interface FeedbackJpaRepository : JpaRepository<Feedback, Long> {
    fun findAllByOrderByIdDesc(pageable: Pageable): Page<Feedback>
}
