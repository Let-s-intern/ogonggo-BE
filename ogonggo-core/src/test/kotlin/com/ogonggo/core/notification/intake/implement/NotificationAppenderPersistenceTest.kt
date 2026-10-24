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
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(NotificationAppender::class)
internal class NotificationAppenderPersistenceTest @Autowired constructor(
    private val notificationAppender: NotificationAppender,
    private val notificationRepository: NotificationJpaRepository,
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
    @DisplayName("일반 알림에는 업무 기한을 만들지 않고 명시한 관리자 기한만 저장한다")
    fun `명시한 업무 기한만 저장한다`() {
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
        assertEquals(null, saved.getValue("channel:KAKAO").dispatchExpiresAt)
        assertEquals(null, saved.getValue("channel:EMAIL").dispatchExpiresAt)
        assertEquals(null, saved.getValue("channel:FCM").dispatchExpiresAt)
        assertEquals(NOW.plusHours(24), saved.getValue("admin").dispatchExpiresAt)
    }

    @Test
    @DisplayName("중복 적재는 기존 알림 기한을 보존하고 새 알림의 null 기한도 유지한다")
    fun `새 일반 알림에는 기한을 자동으로 추가하지 않는다`() {
        // given
        val original = notification("original").copy(scheduledAt = NOW, dispatchExpiresAt = NOW.plusHours(1))
        notificationAppender.append(original)

        // when
        val duplicate = notificationAppender.append(original.copy(dispatchExpiresAt = NOW.plusHours(2)))
        notificationAppender.append(original.copy(deduplicationKey = "new", dispatchExpiresAt = null))

        // then
        assertFalse(duplicate)
        val saved = notificationRepository.findAll().associateBy { it.deduplicationKey }
        assertEquals(NOW.plusHours(1), saved.getValue("original").dispatchExpiresAt)
        assertEquals(null, saved.getValue("new").dispatchExpiresAt)
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 6, 12, 0)
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
