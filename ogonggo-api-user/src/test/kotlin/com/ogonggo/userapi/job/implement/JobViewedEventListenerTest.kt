package com.ogonggo.userapi.job.implement

import com.ogonggo.core.job.implement.JobMetricManager
import com.ogonggo.userapi.job.business.JobViewedEvent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class JobViewedEventListenerTest {

    private val jobMetricManager = Mockito.mock(JobMetricManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZONE)
    private val listener = JobViewedEventListener(jobMetricManager, clock)

    @Test
    fun `조회 이벤트를 받으면 현재 시각으로 조회 수를 증가시킨다`() {
        listener.handle(JobViewedEvent(1L))

        Mockito.verify(jobMetricManager).increaseViewCount(1L, LocalDateTime.now(clock))
    }

    @Test
    fun `지표 기록이 실패해도 예외를 전파하지 않는다`() {
        Mockito.doThrow(IllegalStateException("지표 저장 실패"))
            .`when`(jobMetricManager).increaseViewCount(1L, LocalDateTime.now(clock))

        assertDoesNotThrow { listener.handle(JobViewedEvent(1L)) }
    }

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
