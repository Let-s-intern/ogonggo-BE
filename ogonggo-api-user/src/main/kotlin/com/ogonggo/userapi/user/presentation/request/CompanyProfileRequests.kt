package com.ogonggo.userapi.user.presentation.request

import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.core.user.domain.CompanyEmployeeCountRange
import com.ogonggo.core.user.domain.CompanyManagerDuty
import com.ogonggo.core.user.domain.CompanyType
import com.ogonggo.core.user.implement.dto.CompanyManagerInfoUpdateDto
import com.ogonggo.userapi.error.InvalidRequestFieldException
import com.ogonggo.userapi.user.business.CompanyBasicInfoCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

/**
 * 마이페이지의 기본 정보(기관명·로고)를 함께 교체한다.
 * 로고를 빼거나 null로 보내면 지운다.
 *
 * 가입 때 받은 기업 정보(번호 종류부터 홈페이지까지)는 빼거나 null로 보내면 저장된 값을 그대로 둔다.
 * 이 값을 모르는 이전 화면도 같은 API를 부르므로, 빼면 지우는 로고와 달리 지우지 않는다. 형식은 가입과 같다.
 */
data class ReplaceMyCompanyBasicInfoRequest(
    @field:NotBlank
    @field:Size(max = 150)
    @Schema(description = "기관명", example = "렛츠커리어")
    val organizationName: String,

    @field:Size(max = 36)
    @Schema(
        description = "기업 로고. 이미지 업로드(POST /api/v1/images) 응답의 이미지 식별자이며, " +
            "로고를 바꾸지 않을 때는 내 정보 조회의 companyProfile.logoImageId를 그대로 보낸다.",
        example = "7f3c2a1e-9b4d-4c8e-a6f1-2d5b8e0c9a47",
    )
    val logoImageId: String? = null,

    @Schema(
        description = "사업자 번호 종류. 해외 기업이면 OVERSEAS이고 businessNumber를 보내지 않는다. " +
            "보내면 businessNumber도 함께 바꾸고, 빼면 번호 종류와 번호를 그대로 둔다.",
    )
    val businessNumberType: CompanyBusinessNumberType? = null,

    @field:Size(max = 13)
    @field:Pattern(regexp = "^[0-9]+$", message = "숫자만 입력할 수 있습니다.")
    @Schema(
        description = "하이픈 없이 숫자만. 사업자등록번호는 10자리, 법인등록번호는 13자리다. " +
            "businessNumberType 없이 보내거나 종류와 자릿수가 맞지 않으면 400이다.",
        example = "1234567890",
    )
    val businessNumber: String? = null,

    @Schema(description = "기업 형태. 빼면 그대로 둔다.")
    val companyType: CompanyType? = null,

    @Schema(description = "직원 수 구간. 빼면 그대로 둔다.")
    val employeeCountRange: CompanyEmployeeCountRange? = null,

    @field:Size(max = 100)
    @Schema(description = "산업. 공고의 industry와 같은 문자열이다. 빼면 그대로 둔다.", example = "IT·정보통신")
    val industry: String? = null,

    @field:Size(max = 255)
    @Schema(description = "주소. 빼면 그대로 둔다.", example = "서울특별시 마포구 양화로 1")
    val address: String? = null,

    @field:Size(max = 2048)
    @field:URL
    @Schema(description = "홈페이지. 스킴을 포함한 URL이다. 빼면 그대로 둔다.", example = "https://www.letscareer.co.kr")
    val homepageUrl: String? = null,
) {
    fun toCommand(): CompanyBasicInfoCommand = CompanyBasicInfoCommand(
        organizationName = organizationName,
        logoImageId = validOptionalText("logoImageId", logoImageId),
        businessNumberType = businessNumberType,
        businessNumber = validCompanyBusinessNumber(businessNumberType, businessNumber),
        companyType = companyType,
        employeeCountRange = employeeCountRange,
        industry = validOptionalText("industry", industry),
        address = validOptionalText("address", address),
        homepageUrl = validOptionalText("homepageUrl", homepageUrl),
    )
}

/**
 * 마이페이지의 담당자 정보(이름·연락처·수신 이메일)를 함께 교체한다. 로그인 이메일과 비밀번호는 담지 않는다.
 * 연락처·수신 이메일을 빼거나 null로 보내면 저장된 값을 비운다.
 *
 * 가입 때 받은 직책·담당 업무·추천인은 빼거나 null로 보내면 저장된 값을 그대로 둔다.
 * 이 값을 모르는 이전 화면도 같은 API를 부르기 때문이다. 형식은 가입과 같다.
 */
data class ReplaceMyCompanyManagerInfoRequest(
    @field:NotBlank
    @field:Size(max = 100)
    @Schema(description = "담당자 이름", example = "김담당")
    val managerName: String,

    @field:Size(max = 20)
    @field:Pattern(regexp = "^[0-9-]+$", message = "숫자와 하이픈(-)만 입력할 수 있습니다.")
    @Schema(description = "담당자 연락처. 숫자와 하이픈(-)만 받는다.", example = "010-1234-5678")
    val managerPhone: String? = null,

    @field:Size(max = 320)
    @field:Email
    @Schema(description = "로그인 이메일과 따로 받는 정보 수신용 이메일", example = "hr@example.com")
    val notificationEmail: String? = null,

    @field:Size(max = 50)
    @Schema(description = "담당자 직책. 빼면 그대로 둔다.", example = "인사팀 매니저")
    val managerTitle: String? = null,

    @Schema(description = "담당 업무. 빼면 그대로 둔다.")
    val managerDuty: CompanyManagerDuty? = null,

    @field:Size(max = 100)
    @Schema(description = "추천인. 빼면 그대로 둔다.", example = "홍길동")
    val recommender: String? = null,
) {
    fun toCommand(): CompanyManagerInfoUpdateDto = CompanyManagerInfoUpdateDto(
        managerName = managerName,
        managerPhone = managerPhone,
        notificationEmail = validOptionalText("notificationEmail", notificationEmail),
        managerTitle = validOptionalText("managerTitle", managerTitle),
        managerDuty = managerDuty,
        recommender = validOptionalText("recommender", recommender),
    )
}

/**
 * 번호는 종류가 있어야 뜻이 있고 종류마다 자릿수가 다르다. 도메인도 막지만 요청 단계에서 어느 필드가 틀렸는지 알린다.
 * 기업 회원가입과 기본 정보 수정이 같은 규칙을 쓴다.
 */
internal fun validCompanyBusinessNumber(type: CompanyBusinessNumberType?, businessNumber: String?): String? {
    if (type == null) {
        if (businessNumber == null) {
            return null
        }
        throw InvalidRequestFieldException("businessNumberType", "사업자 번호를 보내면 번호 종류도 보내야 합니다.")
    }
    if (!type.accepts(businessNumber)) {
        val reason = type.digitCount?.let { "${type.desc}는 숫자 ${it}자리여야 합니다." }
            ?: "해외 기업은 사업자 번호를 보낼 수 없습니다."
        throw InvalidRequestFieldException("businessNumber", reason)
    }
    return businessNumber
}

/** 빈 문자열은 `@Email`·`@Size`를 통과하지만 도메인은 공백 값을 막으므로, 요청 단계에서 어느 필드인지 알린다. */
private fun validOptionalText(field: String, value: String?): String? {
    if (value != null && value.isBlank()) {
        throw InvalidRequestFieldException(field, "공백일 수 없습니다.")
    }
    return value
}
