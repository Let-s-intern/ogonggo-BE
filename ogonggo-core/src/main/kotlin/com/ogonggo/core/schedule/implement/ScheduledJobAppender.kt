package com.ogonggo.core.schedule.implement

import com.ogonggo.core.schedule.domain.ScheduledJob
import com.ogonggo.core.schedule.implement.dto.ScheduledJobDefinition
import com.ogonggo.core.schedule.persistence.ScheduledJobJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class ScheduledJobAppender internal constructor(
    private val scheduledJobRepository: ScheduledJobJpaRepository,
) {

    /**
     * 행이 없을 때만 기본값으로 켜진 채 만든다. 이미 있으면 운영자가 바꾼 값을 지키려고 그대로 둔다.
     *
     * 유니크 제약 위반을 삼키므로 호출자가 트랜잭션을 열어 둔 채로 부르면 안 된다.
     */
    fun appendIfAbsent(definition: ScheduledJobDefinition) {
        if (scheduledJobRepository.existsByName(definition.name)) {
            return
        }

        try {
            scheduledJobRepository.saveAndFlush(
                ScheduledJob(
                    name = definition.name,
                    cron = definition.defaultCron,
                    enabled = true,
                    description = definition.description,
                ),
            )
        } catch (exception: DataIntegrityViolationException) {
            // 태스크 여럿이 동시에 기동하면 먼저 만든 쪽을 남긴다.
        }
    }
}
