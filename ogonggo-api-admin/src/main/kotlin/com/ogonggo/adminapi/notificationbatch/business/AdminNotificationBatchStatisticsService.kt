package com.ogonggo.adminapi.notificationbatch.business

import com.ogonggo.adminapi.error.InvalidRequestParameterException
import com.ogonggo.adminapi.notificationbatch.presentation.response.AdminNotificationBatchStatisticGroupResponse
import com.ogonggo.adminapi.notificationbatch.presentation.response.AdminNotificationBatchStatisticsResponse
import com.ogonggo.adminapi.notificationbatch.presentation.response.AdminNotificationBatchResultResponse
import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import com.ogonggo.core.notificationbatch.implement.NotificationBatchStatisticsReader
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime
import java.time.Duration

@Service
class AdminNotificationBatchStatisticsService(
    private val reader: NotificationBatchStatisticsReader,
    private val clock: Clock,
) {
    fun read(from: LocalDateTime?, to: LocalDateTime?, targetType: NotificationBatchTargetType?): AdminNotificationBatchStatisticsResponse {
        val now = LocalDateTime.now(clock)
        if ((from == null) != (to == null)) throw InvalidRequestParameterException("from, to", "시작과 종료 시각을 함께 보내야 합니다.")
        val start = from ?: now.minusDays(30)
        val end = to ?: now
        if (!start.isBefore(end)) throw InvalidRequestParameterException("from, to", "시작 시각은 종료 시각보다 빨라야 합니다.")
        if (start.isBefore(now.minusDays(30)) || end.isAfter(now.plusDays(1)) || Duration.between(start, end) > Duration.ofDays(30)) {
            throw InvalidRequestParameterException("from, to", "최근 30일 이내에서 최대 30일 범위를 조회할 수 있습니다.")
        }
        val queryEnd = if (to == null) now.plusSeconds(1) else end
        val groups = reader.read(start, queryEnd, targetType, now).groupBy {
            it.result.batch.targetType to if (it.result.batch.topicName == null) NotificationRecipientType.DIRECT else NotificationRecipientType.TOPIC
        }.map { (key, rows) ->
            val results = rows.map { AdminNotificationBatchResultResponse.from(it.result) }
            val eligible = rows.sumOf { it.readEligibleUserCount }
            val read = rows.sumOf { it.readUserCount }
            AdminNotificationBatchStatisticGroupResponse(
                targetType = key.first, deliveryTargetType = key.second,
                batchCount = rows.size.toLong(),
                targetUserCount = rows.sumOf { it.result.batch.targetUserCount },
                recipientUserCount = rows.sumOf { it.result.batch.recipientUserCount },
                enqueuedCount = rows.sumOf { it.result.batch.enqueuedCount },
                sentCount = results.sumOf { it.sentCount }, failedCount = results.sumOf { it.failedCount },
                unknownCount = results.sumOf { it.unknownCount }, pendingCount = results.sumOf { it.pendingCount },
                expiredCount = results.sumOf { it.expiredCount },
                readUserCount = read, readEligibleUserCount = eligible,
                readRate = if (eligible == 0L) null else read.toDouble() / eligible,
            )
        }.sortedWith(compareBy({ it.targetType.name }, { it.deliveryTargetType.name }))
        return AdminNotificationBatchStatisticsResponse(start, end, groups)
    }
}
