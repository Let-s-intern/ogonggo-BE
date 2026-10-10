package com.ogonggo.userapi.notification.delivery.implement

import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto

/** 채널별 provider 발송 계약. 각 채널 adapter가 접수 확인·명시 실패·결과 미확정을 구분한다. */
internal interface NotificationSender {
    val channel: NotificationChannel
    fun send(notification: NotificationMessageDto): NotificationDeliveryResult
}
