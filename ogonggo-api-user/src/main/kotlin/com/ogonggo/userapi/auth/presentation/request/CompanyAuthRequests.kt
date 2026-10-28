package com.ogonggo.userapi.auth.presentation.request

import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.core.user.domain.CompanyEmployeeCountRange
import com.ogonggo.core.user.domain.CompanyManagerDuty
import com.ogonggo.core.user.domain.CompanyType
import com.ogonggo.userapi.auth.business.CompanySignInCommand
import com.ogonggo.userapi.auth.business.CompanySignUpCommand
import com.ogonggo.userapi.error.InvalidRequestFieldException
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

/**
 * 기관명 아래의 기업·담당자 정보는 모두 선택이다. 가입 화면이 이 값을 받기 전에 배포된 화면도 같은 API를 부르기 때문이다.
 * 어느 값이 필수인지는 가입 화면이 지키고, 서버는 보낸 값의 형식만 검사한다.
 */
data class CompanySignUpRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, max = 64)
    val password: String,
    @field:NotBlank
    @field:Size(max = 150)
    val organizationName: String,
    @field:NotBlank
    @field:Size(max = 100)
    val managerName: String,

    @Schema(description = "사업자 번호 종류. 해외 기업이면 OVERSEAS이고 businessNumber를 보내지 않는다.")
    val businessNumberType: CompanyBusinessNumberType? = null,

    @field:Size(max = 13)
    @field:Pattern(regexp = "^[0-9]+$", message = "숫자만 입력할 수 있습니다.")
    @Schema(
        description = "하이픈 없이 숫자만. 사업자등록번호는 10자리, 법인등록번호는 13자리다. " +
            "businessNumberType 없이 보내거나 종류와 자릿수가 맞지 않으면 400이다.",
        example = "1234567890",
    )
    val businessNumber: String? = null,

    @Schema(description = "기업 형태")
    val companyType: CompanyType? = null,

    @Schema(description = "직원 수 구간")
    val employeeCountRange: CompanyEmployeeCountRange? = null,

    @field:Size(max = 100)
    @Schema(description = "산업. 공고의 industry와 같은 문자열이다.", example = "IT·정보통신")
    val industry: String? = null,

    @field:Size(max = 255)
    @Schema(description = "주소", example = "서울특별시 마포구 양화로 1")
    val address: String? = null,

    @field:Size(max = 2048)
    @field:URL
    @Schema(description = "홈페이지. 스킴을 포함한 URL이다.", example = "https://www.letscareer.co.kr")
    val homepageUrl: String? = null,

    @field:Size(max = 50)
    @Schema(description = "담당자 직책", example = "인사팀 매니저")
    val managerTitle: String? = null,

    @Schema(description = "담당 업무")
    val managerDuty: CompanyManagerDuty? = null,

    @field:Size(max = 20)
    @field:Pattern(regexp = "^[0-9-]+$", message = "숫자와 하이픈(-)만 입력할 수 있습니다.")
    @Schema(description = "담당자 연락처. 숫자와 하이픈(-)만 받는다.", example = "010-1234-5678")
    val managerPhone: String? = null,

    @field:Size(max = 100)
    @Schema(description = "추천인", example = "홍길동")
    val recommender: String? = null,
) {
    fun toCommand(): CompanySignUpCommand = CompanySignUpCommand(
        email = email,
        password = password,
        organizationName = organizationName,
        managerName = managerName,
        businessNumberType = businessNumberType,
        businessNumber = validBusinessNumber(),
        companyType = companyType,
        employeeCountRange = employeeCountRange,
        industry = validOptionalText("industry", industry),
        address = validOptionalText("address", address),
        homepageUrl = validOptionalText("homepageUrl", homepageUrl),
        managerTitle = validOptionalText("managerTitle", managerTitle),
        managerDuty = managerDuty,
        managerPhone = managerPhone,
        recommender = validOptionalText("recommender", recommender),
    )

    /** 번호는 종류가 있어야 뜻이 있고 종류마다 자릿수가 다르다. 도메인도 막지만 요청 단계에서 어느 필드가 틀렸는지 알린다. */
    private fun validBusinessNumber(): String? {
        val type = businessNumberType
            ?: if (businessNumber == null) {
                return null
            } else {
                throw InvalidRequestFieldException("businessNumberType", "사업자 번호를 보내면 번호 종류도 보내야 합니다.")
            }
        if (!type.accepts(businessNumber)) {
            val reason = type.digitCount?.let { "${type.desc}는 숫자 ${it}자리여야 합니다." }
                ?: "해외 기업은 사업자 번호를 보낼 수 없습니다."
            throw InvalidRequestFieldException("businessNumber", reason)
        }
        return businessNumber
    }
}

data class CompanySignInRequest(
    @field:NotBlank val email: String,
    @field:NotBlank val password: String,
) {
    fun toCommand(): CompanySignInCommand = CompanySignInCommand(email = email, password = password)
}

/** 빈 문자열은 `@Size`·`@URL`을 통과하지만 도메인은 공백 값을 막으므로, 요청 단계에서 어느 필드인지 알린다. */
private fun validOptionalText(field: String, value: String?): String? {
    if (value != null && value.isBlank()) {
        throw InvalidRequestFieldException(field, "공백일 수 없습니다.")
    }
    return value
}
