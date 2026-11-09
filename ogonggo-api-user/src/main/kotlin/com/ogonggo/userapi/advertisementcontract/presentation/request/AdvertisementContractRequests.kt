package com.ogonggo.userapi.advertisementcontract.presentation.request

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.userapi.advertisementcontract.business.CreateAdvertisementContractCommand
import com.ogonggo.userapi.user.presentation.request.validCompanyBusinessNumber
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
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

    @field:NotEmpty
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
) {
    fun toCommand(): CreateAdvertisementContractCommand = CreateAdvertisementContractCommand(
        promotionTarget = promotionTarget,
        companyName = companyName,
        recruitmentTitle = recruitmentTitle,
        recruitmentUrl = recruitmentUrl,
        recruitmentDeadline = recruitmentDeadline,
        purpose = purpose,
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
    )
}
