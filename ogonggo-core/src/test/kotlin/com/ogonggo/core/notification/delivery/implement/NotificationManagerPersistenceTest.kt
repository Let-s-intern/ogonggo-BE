package com.ogonggo.core.notification.delivery.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.notification.domain.Notification
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.delivery.implement.NotificationProcessingPolicy
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(NotificationAppender::class, NotificationProcessingPolicy::class, NotificationManager::class)
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

    @ParameterizedTest
    @EnumSource(value = NotificationStatus::class, names = ["SENT", "FAILED", "UNKNOWN"])
    @DisplayName("최근 SENT·FAILED·UNKNOWN 저장만 전역 발송 진행으로 보고 최근 PENDING 적재는 제외한다")
    fun `확정 상태의 수정 시각으로 전역 진행을 확인한다`(status: NotificationStatus) {
        // given
        val now = LocalDateTime.of(2026, 10, 24, 12, 0)
        val cutoff = now.minusMinutes(2)
        append("recent-pending", now)
        append("terminal", now.minusMinutes(3))
        jdbcTemplate.update("update notifications set updated_at = ? where deduplication_key = ?", now, "recent-pending")
        jdbcTemplate.update("update notifications set status = ?, updated_at = ? where deduplication_key = ?", status.name, cutoff, "terminal")

        // when / then
        assertTrue(notificationManager.hasDeliveryProgressSince(cutoff))
        assertFalse(notificationManager.hasDeliveryProgressSince(cutoff.plusSeconds(1)))
    }

    @Test
    @DisplayName("업무 기한이 없는 대기 알림은 지연 시간과 무관하게 발송 대상이다")
    fun `null 업무 기한은 대기 알림을 만료시키지 않는다`() {
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
        jdbcTemplate.update("update notifications set status = 'SENT' where deduplication_key = ?", "sent")

        // when
        val due = notificationManager.findDue(
            now = now,
            limit = 10,
        )

        // then
        assertEquals(listOf("stale", "late-enqueued", "recent", "due-now"), due.map { it.deduplicationKey })
        assertEquals(
            NotificationStatus.PENDING,
            notificationRepository.findAllByDeduplicationKeyIn(listOf("stale")).single().status,
        )
    }

    @Test
    @DisplayName("PENDING 잔량을 전체·발송 가능·업무 기한 초과·미래 건수로 나눈다")
    fun `대기 잔량을 업무 기한 기준으로 집계한다`() {
        val now = LocalDateTime.of(2026, 10, 6, 12, 0)
        append("due", now.minusMinutes(1))
        append("expired", now.minusMinutes(9))
        append("late-enqueued", now.minusMinutes(20), createdAt = now.minusMinutes(1))
        append("future", now.plusMinutes(1))
        append("sent", now.minusMinutes(1))
        jdbcTemplate.update("update notifications set status = 'SENT' where deduplication_key = ?", "sent")

        assertEquals(4L, notificationManager.countPendingTotal())
        assertEquals(3L, notificationManager.countPendingDue(now))
        assertEquals(0L, notificationManager.countPendingExpired(now))
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
        val claimed = notificationManager.claimDue(now, 10).single()
        val recorded = notificationManager.complete(
            notificationId = notification.id!!,
            attemptId = checkNotNull(claimed.processingAttemptId),
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
        val claimed = notificationManager.claimDue(now, 10).single()
        val recorded = notificationManager.complete(
            notificationId = notification.id!!,
            attemptId = checkNotNull(claimed.processingAttemptId),
            result = NotificationDeliveryResult.Failed(
                "-3005",
                NotificationFailureCategory.TEMPLATE_CONFIGURATION,
                "템플릿 승인 상태 오류",
            ),
            now = now.plusSeconds(1),
        )

        // then
        val saved = notificationRepository.findAll().single()
        assertTrue(recorded)
        assertEquals(NotificationStatus.FAILED, saved.status)
        assertEquals("-3005", saved.resultCode)
        assertEquals(NotificationFailureCategory.TEMPLATE_CONFIGURATION, saved.resultCategory)
        assertEquals("템플릿 승인 상태 오류", saved.resultDetail)
        assertEquals(null, saved.sentAt)
        val nextDay = now.plusDays(1)
        assertTrue(
            notificationManager.findDue(
                now = nextDay,
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
        val claimed = notificationManager.claimDue(now, 10).single()
        val recorded = notificationManager.complete(
            notificationId = notification.id!!,
            attemptId = checkNotNull(claimed.processingAttemptId),
            result = NotificationDeliveryResult.Unknown(
                "-1005",
                NotificationFailureCategory.DUPLICATE_REQUEST,
                "동일한 멱등성 키로 10분 이내 요청됨",
            ),
            now = now.plusSeconds(1),
        )

        // then
        val saved = notificationRepository.findAll().single()
        assertTrue(recorded)
        assertEquals(NotificationStatus.UNKNOWN, saved.status)
        assertEquals("-1005", saved.resultCode)
        assertEquals(NotificationFailureCategory.DUPLICATE_REQUEST, saved.resultCategory)
        assertEquals("동일한 멱등성 키로 10분 이내 요청됨", saved.resultDetail)
        assertTrue(
            notificationManager.findDue(
                now = now.plusDays(1),
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
        val sentAttempt = notificationManager.claimDue(now, 10).single { it.notificationId == sentId }
        notificationManager.complete(sentId, checkNotNull(sentAttempt.processingAttemptId), NotificationDeliveryResult.Sent("provider-2"), now.plusSeconds(1))

        // when
        val deleted = notificationManager.deletePendingByDeduplicationKeyPrefix(prefix)

        // then
        val remaining = notificationRepository.findAll().associateBy { it.deduplicationKey }
        assertEquals(1, deleted)
        assertFalse("${prefix}pending:user:17" in remaining)
        assertEquals(NotificationStatus.SENT, remaining["${prefix}sent:user:18"]?.status)
        assertEquals(NotificationStatus.PENDING, remaining["other-job:pending"]?.status)
    }

    @Test
    @DisplayName("어드민 발송은 지정 기한까지 처리하고 일반 알림은 업무 기한 없이 대기한다")
    fun `어드민 대기 기한은 일반 알림과 독립적이다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 22, 12, 0)
        append("old-comment", now.minusMinutes(9))
        append("new-comment", now.minusMinutes(1))
        listOf("admin-due" to now.plusHours(23), "admin-expired" to now).forEach { (key, expires) ->
            notificationAppender.append(NotificationAppendDto(
                deduplicationKey = key, channel = NotificationChannel.FCM, templateCode = "admin_notification",
                recipientAddress = "test-token", payloadJson = "{}", scheduledAt = now.minusHours(1),
                recipientUserId = 17, batchId = "test-batch", dispatchExpiresAt = expires,
            ))
            jdbcTemplate.update("update notifications set created_at = ? where deduplication_key = ?", now.minusHours(1), key)
        }

        // when
        val due = notificationManager.findDue(now, 10)

        // then
        assertEquals(listOf("old-comment", "new-comment", "admin-due"), due.map { it.deduplicationKey })
        assertEquals(3L, notificationManager.countPendingDue(now))
        assertEquals(1L, notificationManager.countPendingExpired(now))
        assertEquals(4L, notificationManager.countPendingTotal())
    }

    @Test
    @DisplayName("발송 조회와 대기 지표는 채널에 관계없이 저장된 기한과 동일한 만료 경계를 사용한다")
    fun `채널별 저장 기한으로 발송 대상과 기한 초과 건수를 구분한다`() {
        // given
        val now = LocalDateTime.of(2026, 10, 24, 12, 0)
        append("kakao-cutoff", now.minusHours(1), channel = NotificationChannel.KAKAO, dispatchExpiresAt = now)
        append("email-active", now.minusHours(1), channel = NotificationChannel.EMAIL, dispatchExpiresAt = now.plusMinutes(20))
        append("fcm-expired", now.minusHours(1), channel = NotificationChannel.FCM, dispatchExpiresAt = now.minusSeconds(1))
        append("future", now.plusHours(1), channel = NotificationChannel.FCM, dispatchExpiresAt = now.plusHours(2))

        // when
        val due = notificationManager.findDue(now = now, limit = 10)
        val dueCount = notificationManager.countPendingDue(now)
        val expiredCount = notificationManager.countPendingExpired(now)
        val futureCount = notificationManager.countPendingFuture(now)

        // then
        assertEquals(listOf("email-active"), due.map { it.deduplicationKey })
        assertEquals(1L, dueCount)
        assertEquals(2L, expiredCount)
        assertEquals(1L, futureCount)
    }

    private fun append(
        key: String,
        scheduledAt: LocalDateTime,
        createdAt: LocalDateTime = scheduledAt.minusSeconds(1),
        channel: NotificationChannel = NotificationChannel.KAKAO,
        dispatchExpiresAt: LocalDateTime? = null,
    ) {
        // 이전 버전의 기한 없는 행을 재현한다. 신규 적재는 Appender가 기한을 채운다.
        notificationRepository.saveAndFlush(
            Notification(
                deduplicationKey = key,
                channel = channel,
                templateCode = "sign_up_confirm",
                recipientAddress = "01012345678",
                payloadJson = "{\"name\":\"홍길동\"}",
                scheduledAt = scheduledAt,
                recipientUserId = 17,
                dispatchExpiresAt = dispatchExpiresAt,
            ),
        )
        jdbcTemplate.update(
            "update notifications set created_at = ? where deduplication_key = ?",
            createdAt,
            key,
        )
    }
}
