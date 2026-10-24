package com.ogonggo.adminapi.notificationresult.presentation.response

import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryRecordDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notification.domain.NotificationStatus
import java.time.LocalDateTime

data class AdminNotificationRecentResultsResponse(
    val items: List<AdminNotificationRecentResultItemResponse>,
) {
    companion object {
        fun from(records: List<NotificationDeliveryRecordDto>) =
            AdminNotificationRecentResultsResponse(records.map(AdminNotificationRecentResultItemResponse::from))
    }
}

data class AdminNotificationRecentResultItemResponse(
    val notificationId: Long,
    val channel: NotificationChannel,
    val status: NotificationStatus,
    val recipientType: NotificationRecipientType,
    val templateCode: String,
    val recipientUserId: Long?,
    val userNotificationId: Long?,
    val batchId: String?,
    val scheduledAt: LocalDateTime,
    val processingStartedAt: LocalDateTime?,
    val sentAt: LocalDateTime?,
    val providerMessageId: String?,
    val resultCode: String?,
    val resultCategory: NotificationFailureCategory?,
    val resultDetail: String?,
    val attemptCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(record: NotificationDeliveryRecordDto) = AdminNotificationRecentResultItemResponse(
            notificationId = record.notificationId,
            channel = record.channel,
            status = record.status,
            recipientType = record.recipientType,
            templateCode = record.templateCode,
            recipientUserId = record.recipientUserId,
            userNotificationId = record.userNotificationId,
            batchId = record.batchId,
            scheduledAt = record.scheduledAt,
            processingStartedAt = record.processingStartedAt,
            sentAt = record.sentAt,
            providerMessageId = record.providerMessageId,
            resultCode = record.resultCode,
            resultCategory = record.resultCategory,
            resultDetail = record.resultDetail,
            attemptCount = record.attemptCount,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
        )
    }
}
