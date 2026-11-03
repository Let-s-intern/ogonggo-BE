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
    val imageId: String? = null,
    val imageUrl: String? = null,
    val targetUserCount: Long,
    val recipientUserCount: Long,
    val enqueuedCount: Long,
    val skippedNoTokenUserCount: Long,
    val skippedInactiveUserCount: Long,
    val dispatchExpiresAt: LocalDateTime,
    val topicName: String? = null,
    val url: String? = null,
    /** 희망 직무 발송에서 고른 렛츠커리어 직무 선택지(JSON 배열). */
    val targetJobRolesJson: String? = null,
    val advertisement: Boolean = false,
    val skippedNoMarketingConsentUserCount: Long = 0,
)

data class NotificationBatchDto(
    val batchId: String,
    val requestedByUserId: Long,
    val targetType: NotificationBatchTargetType,
    val title: String,
    val body: String,
    val dataJson: String,
    val imageId: String? = null,
    val imageUrl: String? = null,
    val targetUserCount: Long,
    val recipientUserCount: Long,
    val enqueuedCount: Long,
    val skippedNoTokenUserCount: Long,
    val skippedInactiveUserCount: Long,
    val dispatchExpiresAt: LocalDateTime,
    val topicName: String? = null,
    val url: String? = null,
    val createdAt: LocalDateTime,
    val targetJobRolesJson: String? = null,
    val advertisement: Boolean = false,
    val skippedNoMarketingConsentUserCount: Long = 0,
)

data class NotificationBatchStatusCountDto(val status: NotificationStatus, val resultCode: String?, val count: Long)

data class NotificationBatchResultDto(val batch: NotificationBatchDto, val counts: List<NotificationBatchStatusCountDto>, val queriedAt: LocalDateTime)
