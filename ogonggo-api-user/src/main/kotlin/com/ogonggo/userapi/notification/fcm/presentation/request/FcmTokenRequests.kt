package com.ogonggo.userapi.notification.fcm.presentation.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ReplaceFcmTokenRequest(
    @field:NotBlank
    @field:Size(max = 4096)
    val token: String,
)
