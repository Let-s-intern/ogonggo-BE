package com.ogonggo.userapi.advertisementcontract.presentation.request

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractFormAnswer
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.userapi.advertisementcontract.business.CreateAdvertisementContractCommand
import com.ogonggo.userapi.user.presentation.request.validCompanyBusinessNumber
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import java.time.LocalDate

data class CreateAdvertisementContractRequest(
    val promotionTarget: AdvertisementContractPromotionTarget,

    @field:NotBlank
    @field:Size(max = 150)
    val companyName: String,

    @field:NotBlank
    @field:Size(max = 255)
    val recruitmentTitle: String,

    @field:Schema(description = "HTTP 또는 HTTPS 절대 URL")
    @field:NotBlank
    @field:Size(max = 2000)
    @field:URL(regexp = "(?i)^https?://.+$")
    val recruitmentUrl: String,

    val recruitmentDeadline: LocalDate,

    val purpose: AdvertisementContractPurpose,

    @field:Schema(description = "예정 예산(원). 1 이상 정수. 보내지 않으면 정하지 않은 것으로 저장합니다", example = "1500000")
    @field:Positive
    val budgetAmount: Int? = null,

    @field:Schema(description = "희망 광고 카테고리. 고르지 않았으면 빈 배열을 보냅니다")
    @field:Size(max = 7)
    val categories: Set<AdvertisementProductCategory>,

    @field:Size(max = 2000)
    val scheduleRequest: String? = null,

    @field:Size(max = 2000)
    val advertisementRequest: String? = null,

    @field:NotBlank
    @field:Size(max = 50)
    val managerName: String,

    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,

    @field:NotBlank
    @field:Size(max = 20)
    val phoneNumber: String,

    @field:Schema(
        description = "사업자 번호 종류. 선택이며 기업 회원가입과 같은 규칙이다. " +
            "해외 기업이면 OVERSEAS이고 businessNumber를 보내지 않는다.",
    )
    val businessNumberType: CompanyBusinessNumberType? = null,

    @field:Size(max = 13)
    @field:Pattern(regexp = "^[0-9]+$", message = "숫자만 입력할 수 있습니다.")
    @field:Schema(
        description = "하이픈 없이 숫자만. 사업자등록번호는 10자리, 법인등록번호는 13자리다. " +
            "businessNumberType 없이 보내거나 종류와 자릿수가 맞지 않으면 400이다.",
        example = "1234567890",
    )
    val businessNumber: String? = null,

    val invoiceMethod: AdvertisementContractInvoiceMethod,

    val creativeMethod: AdvertisementContractCreativeMethod,

    @field:ArraySchema(
        arraySchema = Schema(
            description = "칸이 따로 없는 폼 문항의 답. 화면에 보인 순서대로 최대 30개. 선택이며 보내지 않거나 빈 배열이면 null로 저장합니다. " +
                "key의 뜻은 서버가 검사하지 않습니다.",
            nullable = true,
        ),
    )
    @field:Valid
    @field:Size(max = 30)
    val formAnswers: List<AdvertisementContractFormAnswerRequest>? = null,
) {
    fun toCommand(): CreateAdvertisementContractCommand = CreateAdvertisementContractCommand(
        promotionTarget = promotionTarget,
        companyName = companyName,
        recruitmentTitle = recruitmentTitle,
        recruitmentUrl = recruitmentUrl,
        recruitmentDeadline = recruitmentDeadline,
        purpose = purpose,
        budgetAmount = budgetAmount,
        categories = categories,
        scheduleRequest = scheduleRequest,
        advertisementRequest = advertisementRequest,
        managerName = managerName,
        email = email,
        phoneNumber = phoneNumber,
        businessNumberType = businessNumberType,
        businessNumber = validCompanyBusinessNumber(businessNumberType, businessNumber),
        invoiceMethod = invoiceMethod,
        creativeMethod = creativeMethod,
        formAnswers = formAnswers?.map(AdvertisementContractFormAnswerRequest::toFormAnswer),
    )
}

data class AdvertisementContractFormAnswerRequest(
    @field:Schema(description = "문항 키. FE가 정하며 서버는 뜻을 검사하지 않습니다", example = "desiredProduct")
    @field:NotBlank
    @field:Size(max = 50)
    val key: String,

    @field:Schema(description = "신청 당시 화면에 보인 문항 이름", example = "희망 상품")
    @field:NotBlank
    @field:Size(max = 50)
    val label: String,

    @field:Schema(description = "답. 여러 개를 고른 문항은 FE가 한 문자열로 합쳐 보냅니다", example = "메인 배너, 카카오톡 CRM")
    @field:NotBlank
    @field:Size(max = 1000)
    val value: String,
) {
    fun toFormAnswer(): AdvertisementContractFormAnswer = AdvertisementContractFormAnswer(key = key, label = label, value = value)
}
