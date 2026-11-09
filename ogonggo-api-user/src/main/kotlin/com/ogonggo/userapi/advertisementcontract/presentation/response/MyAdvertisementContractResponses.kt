package com.ogonggo.userapi.advertisementcontract.presentation.response

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractBudgetRange
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractDetailResult
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractProductResult
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementContractSummaryResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class MyAdvertisementContractSummaryResponse(
    val id: Long,
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentDeadline: LocalDate,
    @field:Schema(description = "신청 때 고른 광고 카테고리")
    val categories: List<AdvertisementProductCategory>,
    @field:Schema(description = "협의로 정한 결제 금액(원). 아직 정하지 않았으면 null", nullable = true)
    val paymentAmount: Int?,
    @field:Schema(description = "신청 일시")
    val registeredAt: LocalDateTime,
) {
    companion object {
        fun from(result: MyAdvertisementContractSummaryResult) = MyAdvertisementContractSummaryResponse(
            id = result.id,
            status = result.status,
            promotionTarget = result.promotionTarget,
            companyName = result.companyName,
            recruitmentTitle = result.recruitmentTitle,
            recruitmentDeadline = result.recruitmentDeadline,
            categories = result.categories,
            paymentAmount = result.paymentAmount,
            registeredAt = result.registeredAt,
        )
    }
}

data class MyAdvertisementContractDetailResponse(
    val id: Long,
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentUrl: String,
    val recruitmentDeadline: LocalDate,
    val purpose: AdvertisementContractPurpose,
    @field:Schema(description = "예정 예산 구간. 고르지 않았으면 null", nullable = true)
    val budgetRange: AdvertisementContractBudgetRange?,
    @field:Schema(description = "신청 때 고른 광고 카테고리")
    val categories: List<AdvertisementProductCategory>,
    @field:Schema(description = "일정 요청사항. 비워 보냈으면 null", nullable = true)
    val scheduleRequest: String?,
    @field:Schema(description = "광고 요청사항. 비워 보냈으면 null", nullable = true)
    val advertisementRequest: String?,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
    @field:Schema(description = "사업자 번호 종류. 보내지 않았으면 null", nullable = true)
    val businessNumberType: CompanyBusinessNumberType?,
    @field:Schema(description = "사업자 번호(숫자만). 보내지 않았거나 해외 기업이면 null", nullable = true)
    val businessNumber: String?,
    val invoiceMethod: AdvertisementContractInvoiceMethod,
    val creativeMethod: AdvertisementContractCreativeMethod,
    @field:Schema(description = "협의로 정한 결제 금액(원). 아직 정하지 않았으면 null", nullable = true)
    val paymentAmount: Int?,
    @field:Schema(description = "신청 일시")
    val registeredAt: LocalDateTime,
    @field:Schema(description = "운영자가 계약에 붙인 광고 상품. 붙인 순서이며 아직 없으면 빈 배열")
    val products: List<MyAdvertisementContractProductResponse>,
) {
    companion object {
        fun from(result: MyAdvertisementContractDetailResult) = MyAdvertisementContractDetailResponse(
            id = result.id,
            status = result.status,
            promotionTarget = result.promotionTarget,
            companyName = result.companyName,
            recruitmentTitle = result.recruitmentTitle,
            recruitmentUrl = result.recruitmentUrl,
            recruitmentDeadline = result.recruitmentDeadline,
            purpose = result.purpose,
            budgetRange = result.budgetRange,
            categories = result.categories,
            scheduleRequest = result.scheduleRequest,
            advertisementRequest = result.advertisementRequest,
            managerName = result.managerName,
            email = result.email,
            phoneNumber = result.phoneNumber,
            businessNumberType = result.businessNumberType,
            businessNumber = result.businessNumber,
            invoiceMethod = result.invoiceMethod,
            creativeMethod = result.creativeMethod,
            paymentAmount = result.paymentAmount,
            registeredAt = result.registeredAt,
            products = result.products.map(MyAdvertisementContractProductResponse::from),
        )
    }
}

data class MyAdvertisementContractProductResponse(
    @field:Schema(description = "계약에 붙인 상품 연결 식별자. 같은 상품을 여러 번 붙이면 연결마다 다르다")
    val contractProductId: Long,
    @field:Schema(description = "조회 시점의 상품 이름")
    val productName: String,
    val category: AdvertisementProductCategory,
    @field:Schema(description = "노출 자리. 외부 채널이거나 자리가 없는 상품은 null", nullable = true)
    val position: AdvertisementProductPosition?,
    @field:Schema(description = "운영자가 상품을 지웠는지. 지운 상품도 계약에 붙어 있으면 이름과 함께 준다")
    val productDeleted: Boolean,
) {
    companion object {
        fun from(result: MyAdvertisementContractProductResult) = MyAdvertisementContractProductResponse(
            contractProductId = result.contractProductId,
            productName = result.productName,
            category = result.category,
            position = result.position,
            productDeleted = result.productDeleted,
        )
    }
}
