package com.ogonggo.core.notification.delivery.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(NotificationAppender::class, NotificationManager::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
internal class NotificationManagerPersistenceTest @Autowired constructor(
    private val notificationAppender: NotificationAppender,
    private val notificationManager: NotificationManager,
    private val notificationRepository: NotificationJpaRepository,
) {

    @BeforeEach
    fun clearCommittedNotifications() {
        notificationRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("예정 시각이 지난 PENDING 알림만 정해진 크기로 읽는다")
    fun `due 알림만 발송 대상으로 조회한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 6, 12, 0)
        append("due-1", now.minusMinutes(1))
        append("due-2", now)
        append("future", now.plusMinutes(1))
        append("sent", now.minusMinutes(2))
        val sentId = notificationRepository.findAllByDeduplicationKeyIn(listOf("sent")).single().id!!
        notificationManager.complete(sentId, NotificationDeliveryResult.Sent("provider-1"), now)

        // when
        val due = notificationManager.findDue(now, limit = 1)

        // then
        assertEquals(1, due.size)
        assertEquals("due-1", due.single().deduplicationKey)
    }

    @Test
    @DisplayName("provider 접수 시 SENT·sent_at·provider ID를 기록하고 발송 스냅샷을 보존한다")
    fun `provider 접수 결과를 기록한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 6, 12, 0)
        append("signup:user:17:KAKAO", now)
        val notification = notificationRepository.findAll().single()
        val sentAt = now.plusSeconds(1)

        // when
        val recorded = notificationManager.complete(
            notificationId = notification.id!!,
            result = NotificationDeliveryResult.Sent("provider-message-1"),
            now = sentAt,
        )

        // then
        val saved = notificationRepository.findAll().single()
        assertTrue(recorded)
        assertEquals(NotificationStatus.SENT, saved.status)
        assertEquals(sentAt, saved.sentAt)
        assertEquals("provider-message-1", saved.providerMessageId)
        assertEquals("01012345678", saved.recipientAddress)
        assertEquals("{\"name\":\"홍길동\"}", saved.payloadJson)
    }

    @Test
    @DisplayName("발송 오류는 사유를 저장해 FAILED로 종료하고 자동 재시도 대상으로 남기지 않는다")
    fun `발송 오류는 한 번 실패 처리한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 6, 12, 0)
        append("signup:user:17:KAKAO", now)
        val notification = notificationRepository.findAll().single()

        // when
        val recorded = notificationManager.complete(
            notificationId = notification.id!!,
            result = NotificationDeliveryResult.Failed("NHN_TRANSPORT_ERROR"),
            now = now.plusSeconds(1),
        )

        // then
        val saved = notificationRepository.findAll().single()
        assertTrue(recorded)
        assertEquals(NotificationStatus.FAILED, saved.status)
        assertEquals("NHN_TRANSPORT_ERROR", saved.resultCode)
        assertEquals(null, saved.sentAt)
        assertTrue(notificationManager.findDue(now.plusDays(1), limit = 10).isEmpty())
    }

    @Test
    @DisplayName("마감일이 바뀌면 공고의 대기 알림만 제거하고 이미 발송된 이력은 보존한다")
    fun `마감 변경은 대기 알림만 삭제한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 6, 12, 0)
        val prefix = "clip-remind:job:7:"
        append("${prefix}pending:user:17", now)
        append("${prefix}sent:user:18", now)
        append("other-job:pending", now)
        val sentId = notificationRepository.findAllByDeduplicationKeyIn(listOf("${prefix}sent:user:18")).single().id!!
        notificationManager.complete(sentId, NotificationDeliveryResult.Sent("provider-2"), now.plusSeconds(1))

        // when
        val deleted = notificationManager.deletePendingByDeduplicationKeyPrefix(prefix)

        // then
        val remaining = notificationRepository.findAll().associateBy { it.deduplicationKey }
        assertEquals(1, deleted)
        assertFalse("${prefix}pending:user:17" in remaining)
        assertEquals(NotificationStatus.SENT, remaining["${prefix}sent:user:18"]?.status)
        assertEquals(NotificationStatus.PENDING, remaining["other-job:pending"]?.status)
    }

    private fun append(key: String, scheduledAt: LocalDateTime) {
        notificationAppender.append(
            NotificationAppendDto(
                deduplicationKey = key,
                channel = NotificationChannel.KAKAO,
                templateCode = "sign_up_confirm",
                recipientAddress = "01012345678",
                payloadJson = "{\"name\":\"홍길동\"}",
                scheduledAt = scheduledAt,
                recipientUserId = 17,
            ),
        )
    }
}
