package com.ogonggo.core.notification.intake.implement.dto

import com.ogonggo.core.notification.domain.NotificationRecipientType
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
    val userNotificationId: Long? = null,
    val recipientType: NotificationRecipientType = NotificationRecipientType.DIRECT,
    val batchId: String? = null,
    /** 명시하면 채널 기본 정책보다 우선한다. null이면 Appender가 기한을 계산해 저장한다. */
    val dispatchExpiresAt: LocalDateTime? = null,
)
