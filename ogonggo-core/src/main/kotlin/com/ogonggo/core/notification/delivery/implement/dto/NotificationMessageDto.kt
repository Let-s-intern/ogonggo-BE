package com.ogonggo.core.notification.delivery.implement.dto

import com.ogonggo.core.notification.domain.NotificationChannel

/** 알림 행에서 provider adapter로 전달할 발송 스냅샷. JPA entity는 외부 연동 계층에 노출하지 않는다. */
data class NotificationMessageDto(
    val notificationId: Long,
    val channel: NotificationChannel,
    val templateCode: String,
    val recipientAddress: String,
    val payloadJson: String,
    val deduplicationKey: String,
)
