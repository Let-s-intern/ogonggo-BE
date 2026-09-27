package com.ogonggo.core.schedule.persistence

import com.ogonggo.core.schedule.domain.ScheduledJob
import org.springframework.data.jpa.repository.JpaRepository

internal interface ScheduledJobJpaRepository : JpaRepository<ScheduledJob, Long> {
    fun findByName(name: String): ScheduledJob?

    fun existsByName(name: String): Boolean
}
