package com.ogonggo.userapi.notification.fcm.presentation.request

import com.ogonggo.userapi.notification.fcm.business.SendFcmTestNotificationCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SendFcmTestNotificationRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,

    @field:NotBlank
    @field:Size(max = 1_000)
    val body: String,

    @field:Size(max = 20)
    val data: Map<String, String> = emptyMap(),
) {
    fun toCommand(): SendFcmTestNotificationCommand = SendFcmTestNotificationCommand(
        title = title,
        body = body,
        data = data,
    )
}
