package com.ogonggo.userapi.notification.intake.implement.reminder

import com.ogonggo.userapi.notification.intake.business.JobBookmarkReminderEnqueueService
import com.ogonggo.userapi.notification.intake.business.dto.ReminderEnqueueResultDto
import com.ogonggo.userapi.scheduling.SchedulerExecutionObserver
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class JobBookmarkReminderSchedulerTest {

    private val now = LocalDateTime.of(2026, 10, 6, 9, 0)

    @Test
    @DisplayName("처리할 일정이 없으면 빈 배치 확인 후 실행을 마친다")
    fun `빈 배치를 확인하면 반복 처리를 종료한다`() {
        // given
        val enqueueService = Mockito.mock(JobBookmarkReminderEnqueueService::class.java)
        Mockito.`when`(enqueueService.enqueueDue(now)).thenReturn(result(workCount = 0))
        val scheduler = scheduler(enqueueService)

        // when
        scheduler.run()

        // then
        Mockito.verify(enqueueService, Mockito.times(1)).enqueueDue(now)
        Mockito.verifyNoMoreInteractions(enqueueService)
    }

    @Test
    @DisplayName("일정이 처리된 배치 다음에 빈 배치를 확인하면 그때 실행을 마친다")
    fun `처리된 배치가 있으면 빈 배치를 만날 때까지 반복한다`() {
        // given
        val enqueueService = Mockito.mock(JobBookmarkReminderEnqueueService::class.java)
        Mockito.`when`(enqueueService.enqueueDue(now))
            .thenReturn(result(workCount = 2), result(workCount = 0))
        val scheduler = scheduler(enqueueService)

        // when
        scheduler.run()

        // then
        Mockito.verify(enqueueService, Mockito.times(2)).enqueueDue(now)
    }

    private fun scheduler(enqueueService: JobBookmarkReminderEnqueueService) = JobBookmarkReminderScheduler(
        enqueueService = enqueueService,
        clock = Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneId.of("Asia/Seoul")),
        executionObserver = SchedulerExecutionObserver(SimpleMeterRegistry()),
    )

    private fun result(workCount: Int) = ReminderEnqueueResultDto(
        workCount = workCount,
        candidateCount = 0,
        queuedCount = 0,
        skippedCount = 0,
    )
}
