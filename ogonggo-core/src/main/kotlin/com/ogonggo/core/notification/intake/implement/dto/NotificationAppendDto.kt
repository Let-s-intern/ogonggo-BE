package com.ogonggo.core.notification.intake.implement.dto

import com.ogonggo.core.notification.domain.NotificationChannel
import java.time.LocalDateTime

/** 업무 모듈이 알림을 적재할 때 사용하는 저장 계약이다. provider 호출 세부정보는 포함하지 않는다. */
data class NotificationAppendDto(
    val deduplicationKey: String,
    val channel: NotificationChannel,
    val templateCode: String,
    val recipientAddress: String,
    val payloadJson: String,
    val scheduledAt: LocalDateTime,
    val recipientUserId: Long? = null,
)
