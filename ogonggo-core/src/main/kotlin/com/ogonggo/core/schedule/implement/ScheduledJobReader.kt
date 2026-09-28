package com.ogonggo.core.schedule.implement

import com.ogonggo.core.schedule.implement.dto.ScheduledJobSettingDto
import com.ogonggo.core.schedule.persistence.ScheduledJobJpaRepository
import org.springframework.stereotype.Component

@Component
class ScheduledJobReader internal constructor(
    private val scheduledJobRepository: ScheduledJobJpaRepository,
) {

    fun read(name: String): ScheduledJobSettingDto? =
        scheduledJobRepository.findByName(name)?.let {
            ScheduledJobSettingDto(name = it.name, cron = it.cron, enabled = it.enabled)
        }
}
