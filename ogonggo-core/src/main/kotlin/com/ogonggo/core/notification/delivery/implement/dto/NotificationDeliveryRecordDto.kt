package com.ogonggo.core.notification.delivery.implement.dto

import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notification.domain.NotificationStatus
import java.time.LocalDateTime

/** 운영 결과 조회용 알림 기록. 수신 주소와 발송 payload는 포함하지 않는다. */
data class NotificationDeliveryRecordDto(
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
)
