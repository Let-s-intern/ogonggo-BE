package com.ogonggo.core.notification.delivery.implement.dto

/** provider adapter의 결과를 core 알림 상태 모델에 전달한다. */
sealed interface NotificationDeliveryResult {
    data class Sent(val providerMessageId: String?) : NotificationDeliveryResult

    data class Failed(val resultCode: String) : NotificationDeliveryResult
}
