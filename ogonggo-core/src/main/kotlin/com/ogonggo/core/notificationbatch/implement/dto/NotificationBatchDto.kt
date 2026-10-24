package com.ogonggo.core.notificationbatch.implement.dto

import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import java.time.LocalDateTime

data class NotificationBatchAppendDto(
    val batchId: String,
    val requestedByUserId: Long,
    val targetType: NotificationBatchTargetType,
    val requestedUserIdsJson: String?,
    val title: String,
    val body: String,
    val dataJson: String,
    val targetUserCount: Long,
    val recipientUserCount: Long,
    val enqueuedCount: Long,
    val skippedNoTokenUserCount: Long,
    val skippedInactiveUserCount: Long,
    val dispatchExpiresAt: LocalDateTime,
    val topicName: String? = null,
    val url: String? = null,
)

data class NotificationBatchDto(
    val batchId: String,
    val requestedByUserId: Long,
    val targetType: NotificationBatchTargetType,
    val title: String,
    val body: String,
    val dataJson: String,
    val targetUserCount: Long,
    val recipientUserCount: Long,
    val enqueuedCount: Long,
    val skippedNoTokenUserCount: Long,
    val skippedInactiveUserCount: Long,
    val dispatchExpiresAt: LocalDateTime,
    val topicName: String? = null,
    val url: String? = null,
    val createdAt: LocalDateTime,
)

data class NotificationBatchStatusCountDto(val status: NotificationStatus, val resultCode: String?, val count: Long)

data class NotificationBatchResultDto(val batch: NotificationBatchDto, val counts: List<NotificationBatchStatusCountDto>, val queriedAt: LocalDateTime)
