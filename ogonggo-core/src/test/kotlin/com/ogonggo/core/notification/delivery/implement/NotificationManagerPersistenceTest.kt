package com.ogonggo.core.notification.delivery.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.springframework.jdbc.core.JdbcTemplate
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
    private val jdbcTemplate: JdbcTemplate,
) {

    @BeforeEach
    fun clearCommittedNotifications() {
        notificationRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("예정 시각 또는 적재 시각부터 8분 안의 PENDING 알림을 발송 대상으로 읽는다")
    fun `멱등성 보호 시간 안의 due 알림만 조회한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 6, 12, 0)
        append("recent", now.minusMinutes(7).minusSeconds(59))
        append("cutoff", now.minusMinutes(8))
        append("stale", now.minusMinutes(8).minusNanos(1))
        append(
            key = "late-enqueued",
            scheduledAt = now.minusMinutes(20),
            createdAt = now.minusMinutes(1),
        )
        append("due-now", now)
        append("future", now.plusMinutes(1))
        append("sent", now.minusMinutes(2))
        val sentId = notificationRepository.findAllByDeduplicationKeyIn(listOf("sent")).single().id!!
        notificationManager.complete(sentId, NotificationDeliveryResult.Sent("provider-1"), now)

        // when
        val due = notificationManager.findDue(
            now = now,
            notBefore = now.minusMinutes(8),
            limit = 10,
        )

        // then
        assertEquals(listOf("late-enqueued", "recent", "due-now"), due.map { it.deduplicationKey })
        assertEquals(
            NotificationStatus.PENDING,
            notificationRepository.findAllByDeduplicationKeyIn(listOf("stale")).single().status,
        )
    }

    @Test
    @DisplayName("PENDING 잔량을 전체·발송 가능·기한 초과·미래 건수로 나눈다")
    fun `대기 잔량을 발송 창 기준으로 집계한다`() {
        val now = LocalDateTime.of(2026, 10, 6, 12, 0)
        append("due", now.minusMinutes(1))
        append("expired", now.minusMinutes(9))
        append("late-enqueued", now.minusMinutes(20), createdAt = now.minusMinutes(1))
        append("future", now.plusMinutes(1))
        append("sent", now.minusMinutes(1))
        val sent = notificationRepository.findAllByDeduplicationKeyIn(listOf("sent")).single()
        notificationManager.complete(sent.id!!, NotificationDeliveryResult.Sent("provider"), now)

        assertEquals(4L, notificationManager.countPendingTotal())
        assertEquals(2L, notificationManager.countPendingDue(now, now.minusMinutes(8)))
        assertEquals(1L, notificationManager.countPendingExpired(now.minusMinutes(8)))
        assertEquals(1L, notificationManager.countPendingFuture(now))
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
    @DisplayName("명시적 발송 거절은 사유를 저장해 FAILED로 종료하고 자동 재시도 대상으로 남기지 않는다")
    fun `명시 거절을 최종 실패 처리한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 6, 12, 0)
        append("signup:user:17:KAKAO", now)
        val notification = notificationRepository.findAll().single()

        // when
        val recorded = notificationManager.complete(
            notificationId = notification.id!!,
            result = NotificationDeliveryResult.Failed(
                "-3005",
                NotificationFailureCategory.TEMPLATE_CONFIGURATION,
            ),
            now = now.plusSeconds(1),
        )

        // then
        val saved = notificationRepository.findAll().single()
        assertTrue(recorded)
        assertEquals(NotificationStatus.FAILED, saved.status)
        assertEquals("-3005", saved.resultCode)
        assertEquals(NotificationFailureCategory.TEMPLATE_CONFIGURATION, saved.resultCategory)
        assertEquals(null, saved.sentAt)
        val nextDay = now.plusDays(1)
        assertTrue(
            notificationManager.findDue(
                now = nextDay,
                notBefore = nextDay.minusMinutes(8),
                limit = 10,
            ).isEmpty(),
        )
    }

    @Test
    @DisplayName("중복 키 응답처럼 접수 여부가 불명확하면 UNKNOWN으로 기록하고 재발송 대상에서 제외한다")
    fun `접수 미확정 결과를 UNKNOWN으로 저장한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 6, 12, 0)
        append("signup:user:18:KAKAO", now)
        val notification = notificationRepository.findAll().single()

        // when
        val recorded = notificationManager.complete(
            notificationId = notification.id!!,
            result = NotificationDeliveryResult.Unknown(
                "-1005",
                NotificationFailureCategory.DUPLICATE_REQUEST,
            ),
            now = now.plusSeconds(1),
        )

        // then
        val saved = notificationRepository.findAll().single()
        assertTrue(recorded)
        assertEquals(NotificationStatus.UNKNOWN, saved.status)
        assertEquals("-1005", saved.resultCode)
        assertEquals(NotificationFailureCategory.DUPLICATE_REQUEST, saved.resultCategory)
        assertTrue(
            notificationManager.findDue(
                now = now.plusDays(1),
                notBefore = now.plusDays(1).minusMinutes(8),
                limit = 10,
            ).isEmpty(),
        )
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

    private fun append(
        key: String,
        scheduledAt: LocalDateTime,
        createdAt: LocalDateTime = scheduledAt.minusSeconds(1),
    ) {
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
        jdbcTemplate.update(
            "update notifications set created_at = ? where deduplication_key = ?",
            createdAt,
            key,
        )
    }
}
