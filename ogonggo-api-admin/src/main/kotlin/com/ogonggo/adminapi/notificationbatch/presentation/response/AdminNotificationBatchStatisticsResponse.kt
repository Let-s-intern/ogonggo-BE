package com.ogonggo.adminapi.notificationbatch.presentation.response

import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminNotificationBatchStatisticsResponse(
    val from: LocalDateTime,
    val to: LocalDateTime,
    val groups: List<AdminNotificationBatchStatisticGroupResponse>,
)

data class AdminNotificationBatchStatisticGroupResponse(
    val targetType: NotificationBatchTargetType,
    val deliveryTargetType: NotificationRecipientType,
    val batchCount: Long,
    val targetUserCount: Long,
    val recipientUserCount: Long,
    val enqueuedCount: Long,
    @field:Schema(description = "FCM 접수된 발송 작업 수. DIRECT는 기기별, TOPIC은 토픽 요청별")
    val sentCount: Long,
    val failedCount: Long,
    val unknownCount: Long,
    val pendingCount: Long,
    val expiredCount: Long,
    @field:Schema(description = "배치마다 중복 없이 읽은 회원 수를 합한 값. 같은 회원이 여러 배치를 읽으면 각각 센다")
    val readUserCount: Long,
    @field:Schema(description = "읽음률 분모. DIRECT는 알림함 행 수, TOPIC은 발송 시점 대상 회원 수")
    val readEligibleUserCount: Long,
    @field:Schema(description = "readUserCount / readEligibleUserCount. 분모가 0이면 null")
    val readRate: Double?,
)
