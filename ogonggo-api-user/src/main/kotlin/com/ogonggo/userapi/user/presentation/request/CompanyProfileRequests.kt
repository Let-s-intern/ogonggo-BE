package com.ogonggo.userapi.user.presentation.request

import com.ogonggo.core.user.implement.dto.CompanyProfileUpdateDto
import com.ogonggo.userapi.error.InvalidRequestFieldException
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

/**
 * 기업 정보 전체를 교체한다. 로그인 이메일과 비밀번호는 담지 않는다.
 * 선택 값을 빼거나 null로 보내면 저장된 값을 비운다.
 */
data class ReplaceMyCompanyProfileRequest(
    @field:NotBlank
    @field:Size(max = 150)
    @Schema(description = "기관명", example = "렛츠커리어")
    val organizationName: String,

    @field:NotBlank
    @field:Size(max = 100)
    @Schema(description = "담당자 이름", example = "김담당")
    val managerName: String,

    @field:Size(max = 2048)
    @field:URL
    @Schema(description = "기업 로고 이미지 주소", example = "https://cdn.example.com/logo.png")
    val logoUrl: String? = null,

    @field:Size(max = 20)
    @field:Pattern(regexp = "^[0-9-]+$", message = "숫자와 하이픈(-)만 입력할 수 있습니다.")
    @Schema(description = "담당자 연락처. 숫자와 하이픈(-)만 받는다.", example = "010-1234-5678")
    val managerPhone: String? = null,

    @field:Size(max = 320)
    @field:Email
    @Schema(description = "로그인 이메일과 따로 받는 정보 수신용 이메일", example = "hr@example.com")
    val notificationEmail: String? = null,
) {
    fun toCommand(): CompanyProfileUpdateDto = CompanyProfileUpdateDto(
        organizationName = organizationName,
        managerName = managerName,
        logoUrl = validOptionalText("logoUrl", logoUrl),
        managerPhone = managerPhone,
        notificationEmail = validOptionalText("notificationEmail", notificationEmail),
    )
}

/** 빈 문자열은 `@Email`·`@URL`을 통과하지만 도메인은 공백 값을 막으므로, 요청 단계에서 어느 필드인지 알린다. */
private fun validOptionalText(field: String, value: String?): String? {
    if (value != null && value.isBlank()) {
        throw InvalidRequestFieldException(field, "공백일 수 없습니다.")
    }
    return value
}
