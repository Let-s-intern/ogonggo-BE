package com.ogonggo.core.notificationbatch.implement

import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchResultDto
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchStatusCountDto
import com.ogonggo.core.notificationbatch.persistence.NotificationBatchStatisticsQueryRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** 30일 이내 배치의 발송 작업과 회원별 알림함 읽음을 한 번에 조회한다. */
@Component
class NotificationBatchStatisticsReader internal constructor(private val queries: NotificationBatchStatisticsQueryRepository) {
    @Transactional(readOnly = true)
    fun read(from: LocalDateTime, to: LocalDateTime, targetType: NotificationBatchTargetType?, now: LocalDateTime): List<NotificationBatchStatisticDto> {
        val batches = queries.batches(from, to, targetType)
            .filter { now.isBefore(it.createdAt.plusDays(30)) }
        if (batches.isEmpty()) return emptyList()
        val statuses = queries.statusCounts(from, to, targetType)
        val directEligible = queries.directEligibleCounts(from, to, targetType)
        val directRead = queries.directReadCounts(from, to, targetType)
        val topicRead = queries.topicReadCounts(from, to, targetType)
        return batches.map { batch ->
            val counts = statuses[batch.id].orEmpty().map { (status, count) -> NotificationBatchStatusCountDto(status, null, count) }
            val eligible = if (batch.topicName == null) directEligible[batch.id] ?: 0L else batch.targetUserCount
            val read = if (batch.topicName == null) directRead[batch.id] ?: 0L else topicRead[batch.id] ?: 0L
            NotificationBatchStatisticDto(NotificationBatchResultDto(batch.toDto(), counts, now), read, eligible)
        }
    }
}

data class NotificationBatchStatisticDto(
    val result: NotificationBatchResultDto,
    val readUserCount: Long,
    val readEligibleUserCount: Long,
)
