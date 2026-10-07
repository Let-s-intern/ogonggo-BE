package com.ogonggo.userapi.notification.delivery.implement

import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto

/** 채널별 provider 발송 계약. 각 채널 adapter가 자신의 오류 응답을 SENT 또는 FAILED로 변환한다. */
internal interface NotificationSender {
    val channel: NotificationChannel
    fun send(notification: NotificationMessageDto): NotificationDeliveryResult
}
