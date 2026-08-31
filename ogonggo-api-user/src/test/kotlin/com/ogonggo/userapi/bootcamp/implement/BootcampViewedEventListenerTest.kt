package com.ogonggo.userapi.bootcamp.implement

import com.ogonggo.core.bootcamp.implement.BootcampMetricManager
import com.ogonggo.userapi.bootcamp.business.BootcampViewedEvent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class BootcampViewedEventListenerTest {

    private val bootcampMetricManager = Mockito.mock(BootcampMetricManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZONE)
    private val listener = BootcampViewedEventListener(bootcampMetricManager, clock)

    @Test
    fun `조회 이벤트를 받으면 현재 시각으로 조회 수를 증가시킨다`() {
        listener.handle(BootcampViewedEvent(1L))

        Mockito.verify(bootcampMetricManager).increaseViewCount(1L, LocalDateTime.now(clock))
    }

    @Test
    fun `지표 기록이 실패해도 예외를 전파하지 않는다`() {
        Mockito.doThrow(IllegalStateException("지표 저장 실패"))
            .`when`(bootcampMetricManager).increaseViewCount(1L, LocalDateTime.now(clock))

        assertDoesNotThrow { listener.handle(BootcampViewedEvent(1L)) }
    }

    companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
