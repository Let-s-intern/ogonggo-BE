package com.ogonggo.userapi.notification.delivery.implement

import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.userapi.notification.metrics.NotificationMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.task.TaskExecutor
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

class NotificationDispatcherTest {

    private val now = LocalDateTime.of(2026, 10, 6, 12, 0)

    @Test
    @DisplayName("due 알림은 전용 실행기에서 sender를 호출하고 provider 접수 결과를 기록한다")
    fun `대기 알림을 보내고 접수 결과를 기록한다`() {
        // given
        val notification = notification(id = 31)
        val manager = Mockito.mock(NotificationManager::class.java)
        Mockito.`when`(manager.findDue(now, now.minusMinutes(8), 4)).thenReturn(listOf(notification), emptyList())
        Mockito.`when`(manager.complete(31, NotificationDeliveryResult.Sent("provider-31"), now)).thenReturn(true)
        var sendThreadName: String? = null
        val sender = sender(NotificationChannel.KAKAO) {
            sendThreadName = Thread.currentThread().name
            NotificationDeliveryResult.Sent("provider-31")
        }
        val dispatcher = dispatcher(manager, listOf(sender))

        // when
        dispatcher.dispatch()

        // then
        assertEquals("notification-test", sendThreadName)
        Mockito.verify(manager).complete(31, NotificationDeliveryResult.Sent("provider-31"), now)
    }

    @Test
    @DisplayName("sender 예외는 접수 여부 미확정으로 기록해 자동 재발송하지 않는다")
    fun `sender 예외를 미확정 결과로 저장한다`() {
        // given
        val notification = notification(id = 52)
        val manager = Mockito.mock(NotificationManager::class.java)
        Mockito.`when`(manager.findDue(now, now.minusMinutes(8), 4)).thenReturn(listOf(notification), emptyList())
        Mockito.`when`(
            manager.complete(
                52,
                NotificationDeliveryResult.Unknown(
                    "UNCLASSIFIED_SENDER_ERROR",
                    NotificationFailureCategory.APPLICATION_ERROR,
                ),
                now,
            ),
        ).thenReturn(true)
        val dispatcher = dispatcher(manager, listOf(sender(NotificationChannel.KAKAO) { error("sender failed") }))

        // when
        dispatcher.dispatch()

        // then
        Mockito.verify(manager).complete(
            52,
            NotificationDeliveryResult.Unknown(
                "UNCLASSIFIED_SENDER_ERROR",
                NotificationFailureCategory.APPLICATION_ERROR,
            ),
            now,
        )
    }

    @Test
    @DisplayName("결과를 기록하지 못한 알림은 같은 실행에서 다시 보내지 않는다")
    fun `발송 결과 저장 실패 뒤에는 현재 실행을 멈춘다`() {
        // given
        val notification = notification(id = 63)
        val manager = Mockito.mock(NotificationManager::class.java)
        Mockito.`when`(manager.findDue(now, now.minusMinutes(8), 4))
            .thenReturn(listOf(notification), listOf(notification), emptyList())
        Mockito.`when`(manager.complete(63, NotificationDeliveryResult.Sent("provider-63"), now)).thenReturn(false)
        val sendCount = AtomicInteger()
        val dispatcher = dispatcher(manager, listOf(sender(NotificationChannel.KAKAO) {
            sendCount.incrementAndGet()
            NotificationDeliveryResult.Sent("provider-63")
        }))

        // when
        dispatcher.dispatch()

        // then
        assertEquals(1, sendCount.get())
        Mockito.verify(manager, Mockito.times(1)).findDue(now, now.minusMinutes(8), 4)
    }

    @Test
    @DisplayName("등록되지 않은 채널은 즉시 FAILED 사유로 기록한다")
    fun `sender가 없는 채널은 설정 오류로 기록한다`() {
        // given
        val notification = notification(id = 72, channel = NotificationChannel.FCM)
        val manager = Mockito.mock(NotificationManager::class.java)
        Mockito.`when`(manager.findDue(now, now.minusMinutes(8), 4)).thenReturn(listOf(notification), emptyList())
        Mockito.`when`(
            manager.complete(
                72,
                NotificationDeliveryResult.Failed(
                    "CHANNEL_NOT_CONFIGURED",
                    NotificationFailureCategory.CHANNEL_NOT_CONFIGURED,
                ),
                now,
            ),
        )
            .thenReturn(true)
        val dispatcher = dispatcher(manager, emptyList())

        // when
        dispatcher.dispatch()

        // then
        Mockito.verify(manager).complete(
            72,
            NotificationDeliveryResult.Failed(
                "CHANNEL_NOT_CONFIGURED",
                NotificationFailureCategory.CHANNEL_NOT_CONFIGURED,
            ),
            now,
        )
    }

    @Test
    @DisplayName("60초 이상 걸린 실행은 ShedLock 만료 전 지연 경고 대상이다")
    fun `장시간 실행 경고 기준은 60초다`() {
        // given / when / then
        assertEquals(false, NotificationDispatcher.shouldWarnLongRun(59_999))
        assertEquals(true, NotificationDispatcher.shouldWarnLongRun(60_000))
    }

    private fun dispatcher(manager: NotificationManager, senders: List<NotificationSender>): NotificationDispatcher {
        val clock = Clock.fixed(Instant.parse("2026-10-06T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        return NotificationDispatcher(
            notificationManager = manager,
            senders = senders,
            taskExecutor = TaskExecutor { task -> Thread(task, "notification-test").apply { start() } },
            notificationMetrics = NotificationMetrics(SimpleMeterRegistry(), manager, clock),
            clock = clock,
        )
    }

    private fun sender(
        channel: NotificationChannel,
        send: (NotificationMessageDto) -> NotificationDeliveryResult,
    ) = object : NotificationSender {
        override val channel = channel
        override fun send(notification: NotificationMessageDto) = send(notification)
    }

    private fun notification(id: Long, channel: NotificationChannel = NotificationChannel.KAKAO) = NotificationMessageDto(
        notificationId = id,
        channel = channel,
        templateCode = "clip_remind",
        recipientAddress = "01012345678",
        payloadJson = "{}",
        deduplicationKey = "clip-remind:job:7:user:$id:KAKAO",
    )
}
