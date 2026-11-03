package com.ogonggo.core.notification.delivery.implement.dto

import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notification.domain.NotificationChannel
import java.time.LocalDateTime

/** 알림 행에서 provider adapter로 전달할 발송 스냅샷. JPA entity는 외부 연동 계층에 노출하지 않는다. */
data class NotificationMessageDto(
    val notificationId: Long,
    val channel: NotificationChannel,
    val templateCode: String,
    val recipientAddress: String,
    val payloadJson: String,
    val deduplicationKey: String,
    val recipientUserId: Long? = null,
    val userNotificationId: Long? = null,
    val recipientType: NotificationRecipientType = NotificationRecipientType.DIRECT,
    /** 현재 provider 요청의 fencing token. 결과 기록 시 같은 attempt인지 확인한다. */
    val processingAttemptId: String? = null,
    /** 발송 예정 시각. 발송 작업이 꺼낼 때까지 기다린 시간을 잴 때 쓴다. 테스트 대역에서는 생략할 수 있다. */
    val scheduledAt: LocalDateTime? = null,
    /** 관리자 발송 묶음 ID. null이면 개별 알림이다. */
    val batchId: String? = null,
)
