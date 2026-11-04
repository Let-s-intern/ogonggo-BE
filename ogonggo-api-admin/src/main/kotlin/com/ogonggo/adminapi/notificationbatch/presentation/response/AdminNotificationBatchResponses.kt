package com.ogonggo.adminapi.notificationbatch.presentation.response

import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchDto
import com.ogonggo.core.notificationbatch.implement.dto.NotificationBatchResultDto
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
    @field:Schema(description = "광고성 발송 여부")
    val advertisement: Boolean,
    @field:Schema(description = "광고성 발송에서 마케팅 수신에 동의하지 않아 뺀 활성 회원 수. 기업 회원은 동의 항목이 없어 여기에 센다.")
    val noMarketingConsentUserCount: Long,
    val imageId: String?,
    val imageUrl: String?,
) {
    companion object {
        fun from(batch: NotificationBatchDto) = AdminNotificationBatchCreatedResponse(
            batch.batchId, batch.targetUserCount, batch.recipientUserCount, batch.enqueuedCount,
            batch.skippedNoTokenUserCount, batch.skippedInactiveUserCount,
            if (batch.topicName == null) NotificationRecipientType.DIRECT else NotificationRecipientType.TOPIC,
            batch.skippedNoTokenUserCount,
            batch.advertisement, batch.skippedNoMarketingConsentUserCount, batch.imageId, batch.imageUrl,
        )
    }
}

/** 희망 직무 발송에서 고를 수 있는 렛츠커리어 직군과 그 직무. 직군 전체를 고를 때는 allRolesOption 값을 보낸다. */
data class AdminNotificationJobRoleOptionResponse(
    val jobField: String,
    val allRolesOption: String,
    val jobRoles: List<String>,
)

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
    @field:Schema(description = "JOB_ROLES 발송에서 고른 희망 직무 선택지. 다른 대상은 null")
    val targetJobRoles: List<String>?,
    val advertisement: Boolean,
    val noMarketingConsentUserCount: Long,
    val imageId: String?,
    val imageUrl: String?,
) {
    companion object {
        private val jsonMapper = jacksonObjectMapper()

        fun from(result: NotificationBatchResultDto): AdminNotificationBatchResultResponse {
            val batch = result.batch
            fun count(status: NotificationStatus) = result.counts.filter { it.status == status }.sumOf { it.count }
            val expired = !result.queriedAt.isBefore(batch.dispatchExpiresAt)
            val pending = count(NotificationStatus.PENDING)
            val processing = count(NotificationStatus.PROCESSING)
            return AdminNotificationBatchResultResponse(
                deliveryTargetType = if (batch.topicName == null) NotificationRecipientType.DIRECT else NotificationRecipientType.TOPIC,
                noTokenUserCount = batch.skippedNoTokenUserCount,
                targetJobRoles = batch.targetJobRolesJson?.let { jsonMapper.readValue(it, Array<String>::class.java).toList() },
                advertisement = batch.advertisement,
                noMarketingConsentUserCount = batch.skippedNoMarketingConsentUserCount,
                imageId = batch.imageId, imageUrl = batch.imageUrl,
                batchId = batch.batchId, requestedByUserId = batch.requestedByUserId, createdAt = batch.createdAt,
                targetType = batch.targetType, title = batch.title, body = batch.body,
                targetUserCount = batch.targetUserCount, recipientUserCount = batch.recipientUserCount,
                enqueuedCount = batch.enqueuedCount, skippedNoTokenUserCount = batch.skippedNoTokenUserCount,
                skippedInactiveUserCount = batch.skippedInactiveUserCount,
                sentCount = count(NotificationStatus.SENT), failedCount = count(NotificationStatus.FAILED),
                unknownCount = count(NotificationStatus.UNKNOWN),
                pendingCount = processing + if (expired) 0 else pending,
                expiredCount = if (expired) pending else 0,
                completed = processing == 0L && (pending == 0L || expired),
                dispatchExpiresAt = batch.dispatchExpiresAt, resultsAvailableUntil = batch.createdAt.plusDays(30),
                failureReasons = result.counts.filter { it.status == NotificationStatus.FAILED || it.status == NotificationStatus.UNKNOWN }
                    .map { AdminNotificationBatchFailureResponse(it.status, it.resultCode ?: "UNSPECIFIED", it.count) },
            )
        }
    }
}

data class AdminNotificationBatchFailureResponse(val status: NotificationStatus, val resultCode: String, val count: Long)
