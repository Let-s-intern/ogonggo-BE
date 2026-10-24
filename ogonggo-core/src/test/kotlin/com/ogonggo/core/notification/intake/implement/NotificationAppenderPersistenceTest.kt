package com.ogonggo.core.notification.intake.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ContextConfiguration
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@DataJpaTest(properties = [
    "ogonggo.notification.dispatch.windows.email=30m",
    "ogonggo.notification.dispatch.windows.fcm=2m",
])
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(NotificationAppender::class, NotificationDispatchPolicy::class, NotificationAppenderPersistenceTest.FixedTime::class)
internal class NotificationAppenderPersistenceTest @Autowired constructor(
    private val notificationAppender: NotificationAppender,
    private val notificationRepository: NotificationJpaRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Test
    @DisplayName("같은 논리 알림은 템플릿 코드와 발송 채널을 보존하며 한 번만 적재한다")
    fun `같은 deduplication key는 notification 행을 중복 생성하지 않는다`() {
        // given
        val notification = NotificationAppendDto(
            deduplicationKey = "signup-confirm:user:42:KAKAO",
            channel = NotificationChannel.KAKAO,
            templateCode = "sign_up_confirm",
            recipientAddress = "01012345678",
            payloadJson = "{\"name\":\"홍길동\"}",
            scheduledAt = LocalDateTime.of(2026, 10, 6, 12, 0),
        )

        // when
        val firstAppend = notificationAppender.append(notification)
        val duplicateAppend = notificationAppender.append(notification)

        // then
        assertTrue(firstAppend)
        assertFalse(duplicateAppend)
        val saved = notificationRepository.findAllByDeduplicationKeyIn(listOf(notification.deduplicationKey)).single()
        assertEquals("sign_up_confirm", saved.templateCode)
        assertEquals(NotificationChannel.KAKAO, saved.channel)
    }

    @Test
    @DisplayName("마감 일정이 달라지면 기존 알림 행을 보존하면서 새 일정 행을 추가한다")
    fun `새 마감 일정은 기존 알림 행을 취소하지 않고 별도로 적재한다`() {
        // given
        val earlierDeadline = notification(
            deduplicationKey = "clip_remind:user:42:job:7:2026-10-05T09:00:00.000000:KAKAO",
        )
        val changedDeadline = notification(
            deduplicationKey = "clip_remind:user:42:job:7:2026-10-06T09:00:00.000000:KAKAO",
        )

        // when
        notificationAppender.append(earlierDeadline)
        notificationAppender.append(changedDeadline)

        // then
        val saved = notificationRepository.findAllByDeduplicationKeyIn(
            listOf(earlierDeadline.deduplicationKey, changedDeadline.deduplicationKey),
        )
        assertEquals(2, saved.size)
        assertEquals(
            setOf(earlierDeadline.deduplicationKey, changedDeadline.deduplicationKey),
            saved.map { it.deduplicationKey }.toSet(),
        )
    }

    @Test
    @DisplayName("채널별 발송 기한과 명시한 기한을 적재 시 계산해 저장한다")
    fun `늦게 적재한 알림의 채널별 기한과 관리자 지정 기한을 저장한다`() {
        // given
        val notifications = NotificationChannel.entries.map { channel ->
            notification("channel:$channel").copy(channel = channel, scheduledAt = NOW.minusHours(1))
        } + notification("admin").copy(
            channel = NotificationChannel.FCM,
            dispatchExpiresAt = NOW.plusHours(24),
        )

        // when
        notificationAppender.appendAll(notifications)

        // then
        val saved = notificationRepository.findAll().associateBy { it.deduplicationKey }
        assertEquals(NOW.plusMinutes(8), saved.getValue("channel:KAKAO").dispatchExpiresAt)
        assertEquals(NOW.plusMinutes(30), saved.getValue("channel:EMAIL").dispatchExpiresAt)
        assertEquals(NOW.plusMinutes(2), saved.getValue("channel:FCM").dispatchExpiresAt)
        assertEquals(NOW.plusHours(24), saved.getValue("admin").dispatchExpiresAt)
    }

    @Test
    @DisplayName("발송 창을 변경해도 기존 알림과 중복 적재 알림의 기한은 바꾸지 않는다")
    fun `정책 변경은 새 알림에만 적용한다`() {
        // given
        val original = notification("original").copy(scheduledAt = NOW)
        notificationAppender.append(original)
        val changedAppender = NotificationAppender(
            notificationRepository,
            eventPublisher,
            NotificationDispatchPolicy(NotificationDispatchProperties(defaultWindow = Duration.ofHours(1))),
            CLOCK,
        )

        // when
        val duplicate = changedAppender.append(original)
        changedAppender.append(original.copy(deduplicationKey = "new"))

        // then
        assertFalse(duplicate)
        val saved = notificationRepository.findAll().associateBy { it.deduplicationKey }
        assertEquals(NOW.plusMinutes(8), saved.getValue("original").dispatchExpiresAt)
        assertEquals(NOW.plusHours(1), saved.getValue("new").dispatchExpiresAt)
    }

    @TestConfiguration(proxyBeanMethods = false)
    class FixedTime {
        @Bean
        @Primary
        fun notificationClock(): Clock = CLOCK
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 6, 12, 0)
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-10-06T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    }

    private fun notification(deduplicationKey: String) = NotificationAppendDto(
        deduplicationKey = deduplicationKey,
        channel = NotificationChannel.KAKAO,
        templateCode = "clip_remind",
        recipientAddress = "01012345678",
        payloadJson = "{\"name\":\"홍길동\",\"posting-title\":\"백엔드 개발자\"}",
        scheduledAt = LocalDateTime.of(2026, 10, 4, 9, 0),
        recipientUserId = 42,
    )
}
