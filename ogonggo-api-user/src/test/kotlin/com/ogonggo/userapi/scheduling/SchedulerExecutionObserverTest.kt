package com.ogonggo.userapi.scheduling

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SchedulerExecutionObserverTest {

    private val meterRegistry = SimpleMeterRegistry()
    private val observer = SchedulerExecutionObserver(meterRegistry)

    @Test
    fun `스케줄러가 성공하면 성공 횟수와 실행 시간을 기록한다`() {
        // when
        val result = observer.observe("testScheduler") { 3 }

        // then
        assertEquals(3, result)
        assertEquals(
            1.0,
            meterRegistry.get("ogonggo.scheduler.executions")
                .tag("scheduler", "testScheduler")
                .tag("result", "success")
                .counter()
                .count(),
        )
        assertEquals(
            1L,
            meterRegistry.get("ogonggo.scheduler.duration")
                .tag("scheduler", "testScheduler")
                .tag("result", "success")
                .timer()
                .count(),
        )
    }

    @Test
    fun `스케줄러가 실패하면 실패 횟수와 실행 시간을 기록하고 예외를 다시 던진다`() {
        // when & then
        assertThrows(IllegalStateException::class.java) {
            observer.observe("failingScheduler") {
                error("실행 실패")
            }
        }
        assertEquals(
            1.0,
            meterRegistry.get("ogonggo.scheduler.executions")
                .tag("scheduler", "failingScheduler")
                .tag("result", "failure")
                .counter()
                .count(),
        )
        assertEquals(
            1L,
            meterRegistry.get("ogonggo.scheduler.duration")
                .tag("scheduler", "failingScheduler")
                .tag("result", "failure")
                .timer()
                .count(),
        )
    }
}
