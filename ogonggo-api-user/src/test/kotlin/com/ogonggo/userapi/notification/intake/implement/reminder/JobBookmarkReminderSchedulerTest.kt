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
    @DisplayName("처리할 대상이 없으면 첫 조회 후 실행을 마친다")
    fun `빈 배치를 확인하면 반복 처리를 종료한다`() {
        // given
        val enqueueService = Mockito.mock(JobBookmarkReminderEnqueueService::class.java)
        Mockito.`when`(enqueueService.enqueueDue(now, null)).thenReturn(result(workCount = 0))
        val scheduler = scheduler(enqueueService)

        // when
        scheduler.run()

        // then
        Mockito.verify(enqueueService, Mockito.times(1)).enqueueDue(now, null)
        Mockito.verifyNoMoreInteractions(enqueueService)
    }

    @Test
    @DisplayName("페이지가 처리되면 마지막 스크랩 ID부터 다음 페이지를 읽는다")
    fun `처리된 페이지 다음 커서로 이어 읽는다`() {
        // given
        val enqueueService = Mockito.mock(JobBookmarkReminderEnqueueService::class.java)
        Mockito.`when`(enqueueService.enqueueDue(now, null))
            .thenReturn(result(workCount = 1, lastBookmarkId = 500))
        Mockito.`when`(enqueueService.enqueueDue(now, 500))
            .thenReturn(result(workCount = 0))
        val scheduler = scheduler(enqueueService)

        // when
        scheduler.run()

        // then
        Mockito.verify(enqueueService).enqueueDue(now, null)
        Mockito.verify(enqueueService).enqueueDue(now, 500)
    }

    private fun scheduler(enqueueService: JobBookmarkReminderEnqueueService) = JobBookmarkReminderScheduler(
        enqueueService = enqueueService,
        clock = Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneId.of("Asia/Seoul")),
        executionObserver = SchedulerExecutionObserver(SimpleMeterRegistry()),
    )

    private fun result(workCount: Int, lastBookmarkId: Long? = null) = ReminderEnqueueResultDto(
        workCount = workCount,
        candidateCount = 0,
        queuedCount = 0,
        skippedCount = 0,
        lastBookmarkId = lastBookmarkId,
    )
}
