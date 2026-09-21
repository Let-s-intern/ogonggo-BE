package com.ogonggo.userapi.user.presentation.request

import com.ogonggo.core.user.implement.dto.CompanyProfileUpdateDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 기업 정보 전체를 교체한다. 로그인 이메일과 비밀번호는 담지 않는다. */
data class ReplaceMyCompanyProfileRequest(
    @field:NotBlank
    @field:Size(max = 150)
    @Schema(description = "기관명", example = "렛츠커리어")
    val organizationName: String,

    @field:NotBlank
    @field:Size(max = 100)
    @Schema(description = "담당자 이름", example = "김담당")
    val managerName: String,
) {
    fun toCommand(): CompanyProfileUpdateDto = CompanyProfileUpdateDto(
        organizationName = organizationName,
        managerName = managerName,
    )
}
