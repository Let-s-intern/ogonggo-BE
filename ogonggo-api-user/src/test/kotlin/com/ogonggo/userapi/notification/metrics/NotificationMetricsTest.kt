package com.ogonggo.userapi.notification.metrics

import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.intake.implement.event.NotificationEnqueuedEvent
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationMetricsTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val notificationManager = Mockito.mock(NotificationManager::class.java)
    private val now = LocalDateTime.of(2026, 10, 7, 12, 0)
    private val metrics = NotificationMetrics(
        meterRegistry = meterRegistry,
        notificationManager = notificationManager,
        clock = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneId.of("Asia/Seoul")),
    )

    @Test
    fun `커밋된 알림 적재 이벤트를 채널별 누적량으로 집계한다`() {
        metrics.onNotificationEnqueued(NotificationEnqueuedEvent(NotificationChannel.KAKAO, 3))

        assertEquals(
            3.0,
            meterRegistry.get("ogonggo.notification.enqueued")
                .tag("channel", "KAKAO")
                .counter()
                .count(),
        )
    }

    @Test
    fun `rate limit 발송 실패를 결과와 실패 사유 태그로 집계한다`() {
        metrics.recordDelivery(
            NotificationChannel.KAKAO,
            NotificationDeliveryResult.Failed(
                "NHN_HTTP_429",
                NotificationFailureCategory.RATE_LIMITED,
            ),
        )

        assertEquals(
            1.0,
            meterRegistry.get("ogonggo.notification.delivery")
                .tag("channel", "KAKAO")
                .tag("outcome", "FAILED")
                .tag("failure_category", "RATE_LIMITED")
                .counter()
                .count(),
        )
    }

    @Test
    fun `대기 잔량 gauge는 전체 due stale 미래 건수를 구분한다`() {
        Mockito.`when`(notificationManager.countPendingTotal()).thenReturn(11L)
        Mockito.`when`(notificationManager.countPendingDue(now, now.minusMinutes(8))).thenReturn(4L)
        Mockito.`when`(notificationManager.countPendingExpired(now.minusMinutes(8))).thenReturn(5L)
        Mockito.`when`(notificationManager.countPendingFuture(now)).thenReturn(2L)

        assertEquals(11.0, backlog("total"))
        assertEquals(4.0, backlog("due"))
        assertEquals(5.0, backlog("expired"))
        assertEquals(2.0, backlog("future"))
    }

    private fun backlog(state: String) = meterRegistry.get("ogonggo.notification.pending")
        .tag("state", state)
        .gauge()
        .value()
}
