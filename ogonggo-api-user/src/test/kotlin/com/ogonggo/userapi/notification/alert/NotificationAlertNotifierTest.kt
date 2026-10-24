package com.ogonggo.userapi.notification.alert

import com.ogonggo.userapi.slack.SlackProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class NotificationAlertNotifierTest {
    private val sender = Mockito.mock(NotificationAlertSender::class.java)
    private val clock = NotifierClock()
    private val registry = SimpleMeterRegistry()
    private val tasks = ConcurrentLinkedQueue<Runnable>()
    private val notifier = notifier(TaskExecutor { tasks += it })
    private val alert = sampleAlert(NotificationAlertType.ENQUEUE_FAILURE).copy(status = NotificationAlertStatus.FIRING)

    @Test
    @DisplayName("경고를 전용 실행기에 제출해 호출 스레드에서 Slack 요청을 기다리지 않는다")
    fun `전용 실행기로 경고를 전달한다`() {
        // given / when
        notifier.notify(alert)
        // then
        Mockito.verifyNoInteractions(sender)
        assertEquals(1, tasks.size)
        tasks.single().run()
        Mockito.verify(sender).send(alert)
    }

    @Test
    @DisplayName("범위가 달라도 같은 유형과 상태의 경고 1만 건을 동시에 받으면 한 건만 제출한다")
    fun `동시 경고 폭주를 큐 제출 전에 제거한다`() {
        // given
        val webhook = "https://hooks.slack.com/services/test/team/flood-test"
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo(webhook)).andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))
        val properties = NotificationAlertProperties(enabled = true)
        val transport = NotificationAlertSender(builder.build(), SlackProperties(notificationAlerts = webhook), properties, NotificationAlertTemplate(properties), registry, clock)
        val floodNotifier = NotificationAlertNotifier(transport, properties, TaskExecutor { tasks += it }, registry, clock)
        val workers = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        // when
        try {
            val futures = (0 until 8).map { worker ->
                workers.submit {
                    check(start.await(5, TimeUnit.SECONDS))
                    repeat(1_250) { floodNotifier.notify(alert.copy(scope = "scope:$worker:$it")) }
                }
            }
            start.countDown()
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            workers.shutdownNow()
        }
        // then
        assertEquals(1, tasks.size)
        tasks.single().run()
        assertEquals(9_999.0, registry.get("ogonggo.notification.alerts").tags("type", "ENQUEUE_FAILURE", "outcome", "SUPPRESSED").counter().count())
        assertEquals(1.0, registry.get("ogonggo.notification.alerts").tags("type", "ENQUEUE_FAILURE", "outcome", "SENT").counter().count())
        server.verify()
    }

    @Test
    @DisplayName("전송 실패 뒤에도 15분간 같은 경고를 버리고 간격이 지나면 새 한 건을 허용한다")
    fun `실패한 경고도 즉시 재시도하지 않는다`() {
        // given
        Mockito.`when`(sender.send(alert)).thenReturn(NotificationAlertSendResult.FAILED)
        // when
        notifier.notify(alert)
        tasks.remove().run()
        repeat(10_000) { notifier.notify(alert) }
        clock.advance(Duration.ofMinutes(15))
        notifier.notify(alert)
        // then
        assertEquals(1, tasks.size)
        tasks.remove().run()
        Mockito.verify(sender, Mockito.times(2)).send(alert)
    }

    @Test
    @DisplayName("경고 실행기가 거절해도 유실을 허용하고 같은 경고를 반복 제출하지 않는다")
    fun `제출 거절 뒤에도 cooldown을 유지한다`() {
        // given
        val rejected = notifier(TaskExecutor { throw TaskRejectedException("full") })
        // when
        repeat(10_000) { rejected.notify(alert) }
        // then
        Mockito.verifyNoInteractions(sender)
        assertEquals(1.0, registry.get("ogonggo.notification.alerts").tags("type", "ENQUEUE_FAILURE", "outcome", "REJECTED").counter().count())
    }

    @Test
    @DisplayName("발생과 복구를 각각 한 번 허용하되 상태 반복이 발생 억제를 해제하지 않는다")
    fun `장애와 복구가 반복되어도 경고가 폭주하지 않는다`() {
        // given / when
        repeat(10_000) {
            notifier.notify(alert)
            notifier.notify(alert.copy(status = NotificationAlertStatus.RESOLVED))
        }
        // then
        assertEquals(2, tasks.size)
    }

    @Test
    @DisplayName("경고가 비활성화되어 있으면 제출하거나 전송하지 않는다")
    fun `비활성 상태는 작업을 만들지 않는다`() {
        // given
        val disabled = NotificationAlertNotifier(sender, NotificationAlertProperties(), TaskExecutor { tasks += it }, registry, clock)
        // when
        disabled.notify(alert)
        // then
        assertEquals(0, tasks.size)
        Mockito.verifyNoInteractions(sender)
    }

    private fun notifier(executor: TaskExecutor) =
        NotificationAlertNotifier(sender, NotificationAlertProperties(enabled = true), executor, registry, clock)
}

private class NotifierClock : Clock() {
    private var now = Instant.parse("2026-10-24T03:00:00Z")
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneId.of("Asia/Seoul")
    override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    fun advance(duration: Duration) { now = now.plus(duration) }
}
