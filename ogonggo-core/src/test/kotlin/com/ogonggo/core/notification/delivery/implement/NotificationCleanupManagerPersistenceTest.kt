package com.ogonggo.core.notification.delivery.implement

import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(NotificationAppender::class, NotificationCleanupManager::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
internal class NotificationCleanupManagerPersistenceTest @Autowired constructor(
    private val notificationAppender: NotificationAppender,
    private val cleanupManager: NotificationCleanupManager,
    private val notificationRepository: NotificationJpaRepository,
    private val jdbc: JdbcTemplate,
) {

    @Test
    @DisplayName("30일 지난 발송 결과만 제한된 배치로 삭제하고 대기 알림은 보존한다")
    fun `최종 상태의 보관 기간과 배치 크기를 지킨다`() {
        // given
        val cutoff = LocalDateTime.of(2026, 10, 1, 12, 0)
        val old = cutoff.minusDays(1)
        listOf("SENT", "FAILED", "UNKNOWN", "SENT", "FAILED", "UNKNOWN").forEachIndexed { index, status ->
            appendWithState("old-$status-$index", status, old)
        }
        appendWithState("old-PENDING", "PENDING", old)
        appendWithState("recent-SENT", "SENT", cutoff.plusNanos(1_000))

        // when
        val firstBatch = cleanupManager.deleteExpired(cutoff, batchSize = 2)
        val secondBatch = cleanupManager.deleteExpired(cutoff, batchSize = 2)
        val thirdBatch = cleanupManager.deleteExpired(cutoff, batchSize = 2)
        val emptyBatch = cleanupManager.deleteExpired(cutoff, batchSize = 2)

        // then
        assertEquals(2, firstBatch)
        assertEquals(2, secondBatch)
        assertEquals(2, thirdBatch)
        assertEquals(0, emptyBatch)
        assertEquals(
            setOf("old-PENDING", "recent-SENT"),
            notificationRepository.findAll().map { it.deduplicationKey }.toSet(),
        )
    }

    private fun appendWithState(key: String, status: String, updatedAt: LocalDateTime) {
        notificationAppender.append(
            NotificationAppendDto(
                deduplicationKey = key,
                channel = NotificationChannel.KAKAO,
                templateCode = "test-template",
                recipientAddress = "01012345678",
                payloadJson = "{}",
                scheduledAt = updatedAt.minusDays(1),
            ),
        )
        jdbc.update(
            "update notifications set status = ?, updated_at = ? where deduplication_key = ?",
            status,
            Timestamp.valueOf(updatedAt),
            key,
        )
    }
}
