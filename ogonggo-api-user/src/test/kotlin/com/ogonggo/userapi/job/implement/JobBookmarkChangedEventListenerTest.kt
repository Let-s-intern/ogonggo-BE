package com.ogonggo.userapi.job.implement

import com.ogonggo.core.job.implement.JobMetricManager
import com.ogonggo.userapi.job.business.JobBookmarkChangedEvent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class JobBookmarkChangedEventListenerTest {

    private val jobMetricManager = Mockito.mock(JobMetricManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZONE)
    private val listener = JobBookmarkChangedEventListener(jobMetricManager, clock)

    @Test
    fun `북마크 변경 이벤트를 받으면 북마크 수를 다시 센다`() {
        listener.handle(JobBookmarkChangedEvent(1L))

        Mockito.verify(jobMetricManager).syncBookmarkCount(1L, NOW)
    }

    @Test
    fun `지표 갱신이 실패해도 예외를 전파하지 않는다`() {
        Mockito.doThrow(IllegalStateException("지표 저장 실패"))
            .`when`(jobMetricManager).syncBookmarkCount(1L, NOW)

        assertDoesNotThrow { listener.handle(JobBookmarkChangedEvent(1L)) }
    }

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 28, 10, 0)
    }
}
