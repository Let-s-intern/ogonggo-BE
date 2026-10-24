package com.ogonggo.userapi.notification.alert

import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.schedule.implement.ScheduledJobReader
import com.ogonggo.core.schedule.implement.dto.ScheduledJobSettingDto
import com.ogonggo.userapi.notification.delivery.implement.NotificationDispatcher
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal class NotificationDeliveryMonitorTest {
    private val manager = Mockito.mock(NotificationManager::class.java)
    private val notifier = Mockito.mock(NotificationAlertNotifier::class.java)
    private val clock = MonitorClock()
    private val scheduledJobReader = Mockito.mock(ScheduledJobReader::class.java)
    private val monitor = NotificationDeliveryMonitor(manager, notifier, NotificationAlertProperties(enabled = true), clock, scheduledJobReader)

    @Test
    @DisplayName("발송 대기가 2분 이상 지속되고 공용 DB에 최근 확정 처리가 없으면 중단을 경고한다")
    fun `전체 인스턴스의 진행 중단을 감지한다`() {
        // given
        val first = LocalDateTime.now(clock)
        Mockito.`when`(manager.countPendingDue(first)).thenReturn(10L)
        // when
        monitor.check()
        Mockito.verifyNoInteractions(notifier)
        clock.advance(Duration.ofMinutes(2))
        val now = LocalDateTime.now(clock)
        Mockito.`when`(manager.countPendingDue(now)).thenReturn(10L)
        monitor.check()
        // then
        Mockito.verify(notifier).notify(NotificationAlertType.DELIVERY_STALLED, "all", mapOf("due" to "10", "noProgressSeconds" to "120"))
    }

    @Test
    @DisplayName("다른 인스턴스의 최근 DB 확정 처리가 있으면 발송 중단으로 오인하지 않는다")
    fun `진행이 있으면 경고하지 않는다`() {
        // given
        Mockito.`when`(manager.countPendingDue(LocalDateTime.now(clock))).thenReturn(10L)
        monitor.check()
        clock.advance(Duration.ofMinutes(2))
        val now = LocalDateTime.now(clock)
        Mockito.`when`(manager.countPendingDue(now)).thenReturn(20L)
        Mockito.`when`(manager.hasDeliveryProgressSince(now.minusMinutes(2))).thenReturn(true)
        // when
        monitor.check()
        // then
        Mockito.verifyNoInteractions(notifier)
    }

    @Test
    @DisplayName("대기가 사라졌다가 새로 생기면 2분 관찰 시간을 다시 부여한다")
    fun `새 대기를 즉시 장애로 오인하지 않는다`() {
        // given
        Mockito.`when`(manager.countPendingDue(LocalDateTime.now(clock))).thenReturn(10L)
        monitor.check()
        clock.advance(Duration.ofMinutes(3))
        monitor.check()
        clock.advance(Duration.ofMinutes(1))
        Mockito.`when`(manager.countPendingDue(LocalDateTime.now(clock))).thenReturn(10L)
        // when
        monitor.check()
        // then
        Mockito.verifyNoInteractions(notifier)
    }

    @Test
    @DisplayName("감시 DB 조회가 실패해도 다음 감시를 중단하지 않고 안전한 감시 실패 경고를 남긴다")
    fun `조회 실패를 감시 경고로 전환한다`() {
        // given
        Mockito.`when`(manager.countPendingDue(LocalDateTime.now(clock))).thenThrow(IllegalStateException("DB unavailable"))
        // when
        monitor.check()
        // then
        Mockito.verify(notifier).notify(NotificationAlertType.MONITORING_FAILURE, "all", emptyMap())
    }

    @Test
    @DisplayName("운영 경고가 비활성화되어 있으면 감시 DB 조회를 하지 않는다")
    fun `비활성 상태는 감시하지 않는다`() {
        // given
        val disabled = NotificationDeliveryMonitor(manager, notifier, NotificationAlertProperties(), clock, scheduledJobReader)
        // when
        disabled.check()
        // then
        Mockito.verifyNoInteractions(manager, notifier, scheduledJobReader)
    }

    @Test
    @DisplayName("운영자가 발송 작업을 꺼둔 경우 대기 알림이 있어도 장애 경고를 하지 않는다")
    fun `의도적으로 중지한 발송을 장애로 오인하지 않는다`() {
        // given
        Mockito.`when`(scheduledJobReader.read(NotificationDispatcher.SCHEDULER_NAME))
            .thenReturn(ScheduledJobSettingDto(NotificationDispatcher.SCHEDULER_NAME, "* * * * * *", false))
        // when
        monitor.check()
        // then
        Mockito.verifyNoInteractions(manager, notifier)
    }
}

private class MonitorClock : Clock() {
    private var now = Instant.parse("2026-10-24T03:00:00Z")
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneId.of("Asia/Seoul")
    override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    fun advance(duration: Duration) { now = now.plus(duration) }
}
