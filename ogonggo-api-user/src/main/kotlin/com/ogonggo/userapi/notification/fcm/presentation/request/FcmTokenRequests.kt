package com.ogonggo.userapi.notification.fcm.presentation.request

import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

data class ReplaceFcmTokenRequest(
    @field:NotBlank
    @field:Size(max = 4096)
    val token: String,
    @field:Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    @field:Schema(description = "설치별 UUID. 생략하거나 null이면 회원별 기존 클라이언트 슬롯만 교체합니다.", format = "uuid")
    val deviceId: String? = null,
) {
    fun toCommand() = UserFcmTokenRegisterDto(token, deviceId?.let(UUID::fromString))
}

data class DeleteFcmTokenRequest(
    @field:NotBlank
    @field:Size(max = 4096)
    val token: String,
)
