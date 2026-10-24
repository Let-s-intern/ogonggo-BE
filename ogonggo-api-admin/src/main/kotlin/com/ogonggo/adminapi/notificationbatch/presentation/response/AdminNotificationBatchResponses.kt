package com.ogonggo.adminapi.notificationbatch.presentation.response

import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchDto
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchResultDto
import java.time.LocalDateTime
import io.swagger.v3.oas.annotations.media.Schema

data class AdminNotificationBatchCreatedResponse(
    val batchId: String,
    val targetUserCount: Long,
    val recipientUserCount: Long,
    val enqueuedCount: Long,
    @field:Schema(deprecated = true, description = "호환용 별칭. noTokenUserCount 사용") val skippedNoTokenUserCount: Long,
    val skippedInactiveUserCount: Long,
    val deliveryTargetType: NotificationRecipientType,
    val noTokenUserCount: Long,
) {
    companion object {
        fun from(batch: NotificationBatchDto) = AdminNotificationBatchCreatedResponse(
            batch.batchId, batch.targetUserCount, batch.recipientUserCount, batch.enqueuedCount,
            batch.skippedNoTokenUserCount, batch.skippedInactiveUserCount,
            if (batch.topicName == null) NotificationRecipientType.DIRECT else NotificationRecipientType.TOPIC,
            batch.skippedNoTokenUserCount,
        )
    }
}

data class AdminNotificationBatchResultResponse(
    val batchId: String,
    val requestedByUserId: Long,
    val createdAt: LocalDateTime,
    val targetType: NotificationBatchTargetType,
    val title: String,
    val body: String,
    val targetUserCount: Long,
    val recipientUserCount: Long,
    val enqueuedCount: Long,
    @field:Schema(deprecated = true, description = "호환용 별칭. noTokenUserCount 사용") val skippedNoTokenUserCount: Long,
    val skippedInactiveUserCount: Long,
    val sentCount: Long,
    val failedCount: Long,
    val unknownCount: Long,
    val pendingCount: Long,
    val expiredCount: Long,
    val completed: Boolean,
    val dispatchExpiresAt: LocalDateTime,
    val resultsAvailableUntil: LocalDateTime,
    val failureReasons: List<AdminNotificationBatchFailureResponse>,
    val deliveryTargetType: NotificationRecipientType,
    val noTokenUserCount: Long,
) {
    companion object {
        fun from(result: NotificationBatchResultDto): AdminNotificationBatchResultResponse {
            val batch = result.batch
            fun count(status: NotificationStatus) = result.counts.filter { it.status == status }.sumOf { it.count }
            val expired = !result.queriedAt.isBefore(batch.dispatchExpiresAt)
            val pending = count(NotificationStatus.PENDING)
            return AdminNotificationBatchResultResponse(
                deliveryTargetType = if (batch.topicName == null) NotificationRecipientType.DIRECT else NotificationRecipientType.TOPIC,
                noTokenUserCount = batch.skippedNoTokenUserCount,
                batchId = batch.batchId, requestedByUserId = batch.requestedByUserId, createdAt = batch.createdAt,
                targetType = batch.targetType, title = batch.title, body = batch.body,
                targetUserCount = batch.targetUserCount, recipientUserCount = batch.recipientUserCount,
                enqueuedCount = batch.enqueuedCount, skippedNoTokenUserCount = batch.skippedNoTokenUserCount,
                skippedInactiveUserCount = batch.skippedInactiveUserCount,
                sentCount = count(NotificationStatus.SENT), failedCount = count(NotificationStatus.FAILED),
                unknownCount = count(NotificationStatus.UNKNOWN), pendingCount = if (expired) 0 else pending,
                expiredCount = if (expired) pending else 0, completed = pending == 0L || expired,
                dispatchExpiresAt = batch.dispatchExpiresAt, resultsAvailableUntil = batch.createdAt.plusDays(30),
                failureReasons = result.counts.filter { it.status == NotificationStatus.FAILED || it.status == NotificationStatus.UNKNOWN }
                    .map { AdminNotificationBatchFailureResponse(it.status, it.resultCode ?: "UNSPECIFIED", it.count) },
            )
        }
    }
}

data class AdminNotificationBatchFailureResponse(val status: NotificationStatus, val resultCode: String, val count: Long)
