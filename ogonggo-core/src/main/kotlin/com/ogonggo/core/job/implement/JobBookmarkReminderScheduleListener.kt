package com.ogonggo.core.job.implement

import com.ogonggo.core.job.implement.event.JobRecruitmentDeadlineChangedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 마감 변경 이벤트를 동기 처리해 일정 갱신을 공고 변경 트랜잭션에 참여시킨다.
 * core에 두어 user/admin 어느 API에서 발생한 변경이든 동일한 정책으로 저장한다.
 */
@Component
internal class JobBookmarkReminderScheduleListener(
    private val scheduleManager: JobBookmarkReminderScheduleManager,
) {

    @Transactional
    @EventListener
    fun on(event: JobRecruitmentDeadlineChangedEvent) = scheduleManager.apply(event)
}
