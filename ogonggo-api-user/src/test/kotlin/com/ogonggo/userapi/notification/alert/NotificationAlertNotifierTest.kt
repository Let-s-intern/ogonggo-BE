package com.ogonggo.userapi.notification.alert

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException

internal class NotificationAlertNotifierTest {
    @Test
    @DisplayName("경고를 전용 실행기에 제출해 호출 스레드에서 Slack 요청을 기다리지 않는다")
    fun `전용 실행기로 경고를 전달한다`() {
        // given
        val sender = Mockito.mock(NotificationAlertSender::class.java)
        val tasks = mutableListOf<Runnable>()
        val notifier = NotificationAlertNotifier(sender, NotificationAlertProperties(enabled = true), TaskExecutor { tasks += it }, SimpleMeterRegistry())
        val alert = sampleAlert(NotificationAlertType.CLEANUP_BACKLOG)
        // when
        notifier.notify(alert)
        // then
        Mockito.verifyNoInteractions(sender)
        assertEquals(1, tasks.size)
        tasks.single().run()
        Mockito.verify(sender).send(alert)
    }

    @Test
    @DisplayName("경고 실행기 거절을 호출자에게 전파하지 않고 별도 지표로 기록한다")
    fun `제출 거절을 기록한다`() {
        // given
        val sender = Mockito.mock(NotificationAlertSender::class.java)
        val registry = SimpleMeterRegistry()
        val notifier = NotificationAlertNotifier(sender, NotificationAlertProperties(enabled = true), TaskExecutor { throw TaskRejectedException("full") }, registry)
        // when
        notifier.notify(sampleAlert(NotificationAlertType.ENQUEUE_FAILURE))
        // then
        Mockito.verifyNoInteractions(sender)
        assertEquals(1.0, registry.get("ogonggo.notification.alerts").tags("type", "ENQUEUE_FAILURE", "outcome", "REJECTED").counter().count())
    }
}
