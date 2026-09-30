package com.ogonggo.userapi.user.presentation.request

import com.ogonggo.userapi.error.InvalidRequestFieldException
import com.ogonggo.userapi.user.business.PasswordChangeCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 빼거나 null로 보내면 저장된 수신 이메일을 비운다. */
data class ReplaceMyNotificationEmailRequest(
    @field:Size(max = 320)
    @field:Email
    @Schema(description = "오늘의 공고 정보를 받을 이메일", example = "me@example.com")
    val notificationEmail: String? = null,
) {
    /** 빈 문자열은 `@Email`을 통과하지만 도메인은 공백 값을 막으므로, 요청 단계에서 어느 필드인지 알린다. */
    fun toNotificationEmail(): String? {
        if (notificationEmail != null && notificationEmail.isBlank()) {
            throw InvalidRequestFieldException("notificationEmail", "공백일 수 없습니다.")
        }
        return notificationEmail
    }
}

data class ReplaceMyProfileImageRequest(
    @field:NotBlank
    @field:Size(max = 36)
    @Schema(description = "이미지 업로드(POST /api/v1/images) 응답의 이미지 식별자", example = "7f3c2a1e-9b4d-4c8e-a6f1-2d5b8e0c9a47")
    val imageId: String,
)

/**
 * 새 비밀번호의 형식은 비밀번호를 가진 쪽이 검사한다.
 * 일반 회원은 렛츠커리어 규칙(8자 이상, 특수문자 포함)을, 기업 회원은 가입 때와 같은 길이 규칙을 따른다.
 */
data class ChangeMyPasswordRequest(
    @field:NotBlank
    @Schema(description = "기존 비밀번호")
    val currentPassword: String,

    @field:NotBlank
    @field:Size(min = 8, max = 64)
    @Schema(description = "새 비밀번호. 8~64자이며 일반 회원은 특수문자를 하나 이상 포함해야 한다.")
    val newPassword: String,
) {
    fun toCommand(): PasswordChangeCommand = PasswordChangeCommand(
        currentPassword = currentPassword,
        newPassword = newPassword,
    )
}
