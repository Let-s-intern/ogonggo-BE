package com.ogonggo.userapi.advertisementcontract.presentation.request

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractBudgetRange
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.userapi.advertisementcontract.business.CreateAdvertisementContractCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
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

    @field:Schema(description = "예정 예산 구간. 보내지 않으면 고르지 않은 것으로 저장합니다")
    val budgetRange: AdvertisementContractBudgetRange? = null,

    @field:NotEmpty
    @field:Size(max = 6)
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
        budgetRange = budgetRange,
        categories = categories,
        scheduleRequest = scheduleRequest,
        advertisementRequest = advertisementRequest,
        managerName = managerName,
        email = email,
        phoneNumber = phoneNumber,
        invoiceMethod = invoiceMethod,
        creativeMethod = creativeMethod,
    )
}
