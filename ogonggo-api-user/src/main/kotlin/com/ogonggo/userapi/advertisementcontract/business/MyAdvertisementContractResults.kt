package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractFormAnswer
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractReportUnavailableReason
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractPageDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractProductDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import java.time.LocalDate
import java.time.LocalDateTime

data class MyAdvertisementContractPageResult(
    val items: List<MyAdvertisementContractSummaryResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(page: AdvertisementContractPageDto): MyAdvertisementContractPageResult =
            MyAdvertisementContractPageResult(
                items = page.advertisementContracts.map(MyAdvertisementContractSummaryResult::from),
                page = page.page,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
            )
    }
}

data class MyAdvertisementContractSummaryResult(
    val id: Long,
    val source: AdvertisementContractSource,
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentDeadline: LocalDate?,
    val categories: List<AdvertisementProductCategory>,
    val paymentAmount: Int?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(contract: AdvertisementContract): MyAdvertisementContractSummaryResult =
            MyAdvertisementContractSummaryResult(
                id = checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." },
                source = contract.source,
                status = contract.status,
                promotionTarget = contract.promotionTarget,
                companyName = contract.companyName,
                recruitmentTitle = contract.recruitmentTitle,
                recruitmentDeadline = contract.recruitmentDeadline,
                categories = contract.categories.sorted(),
                paymentAmount = contract.paymentAmount,
                registeredAt = contract.createdAt,
            )
    }
}

/**
 * 신청자가 낸 값과 운영자가 정한 상태·결제 금액·상품이다. 회원 식별자처럼 신청자에게 필요 없는 값은 싣지 않는다.
 * 신청 폼에서만 받는 값(공고 링크, 광고 목적, 담당자 등)은 비즈니스 공고 계약이면 null이다.
 */
data class MyAdvertisementContractDetailResult(
    val id: Long,
    val source: AdvertisementContractSource,
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentUrl: String?,
    val recruitmentDeadline: LocalDate?,
    val purpose: AdvertisementContractPurpose?,
    val budgetAmount: Int?,
    val categories: List<AdvertisementProductCategory>,
    val scheduleRequest: String?,
    val advertisementRequest: String?,
    val managerName: String?,
    val email: String?,
    val phoneNumber: String?,
    val businessNumberType: CompanyBusinessNumberType?,
    val businessNumber: String?,
    val invoiceMethod: AdvertisementContractInvoiceMethod?,
    val creativeMethod: AdvertisementContractCreativeMethod?,
    val formAnswers: List<AdvertisementContractFormAnswer>?,
    val paymentAmount: Int?,
    val registeredAt: LocalDateTime,
    val products: List<MyAdvertisementContractProductResult>,
) {
    companion object {
        internal fun from(
            contract: AdvertisementContract,
            products: List<AdvertisementContractProductDto>,
        ): MyAdvertisementContractDetailResult = MyAdvertisementContractDetailResult(
            id = checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." },
            source = contract.source,
            status = contract.status,
            promotionTarget = contract.promotionTarget,
            companyName = contract.companyName,
            recruitmentTitle = contract.recruitmentTitle,
            recruitmentUrl = contract.recruitmentUrl,
            recruitmentDeadline = contract.recruitmentDeadline,
            purpose = contract.purpose,
            budgetAmount = contract.budgetAmount,
            categories = contract.categories.sorted(),
            scheduleRequest = contract.scheduleRequest,
            advertisementRequest = contract.advertisementRequest,
            managerName = contract.managerName,
            email = contract.email,
            phoneNumber = contract.phoneNumber,
            businessNumberType = contract.businessNumberType,
            businessNumber = contract.businessNumber,
            invoiceMethod = contract.invoiceMethod,
            creativeMethod = contract.creativeMethod,
            formAnswers = contract.formAnswers,
            paymentAmount = contract.paymentAmount,
            registeredAt = contract.createdAt,
            products = products.map(MyAdvertisementContractProductResult::from),
        )
    }
}

data class MyAdvertisementContractProductResult(
    val contractProductId: Long,
    val productName: String,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val productDeleted: Boolean,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val reportAvailable: Boolean,
    val reportUnavailableReason: AdvertisementContractReportUnavailableReason?,
) {
    companion object {
        internal fun from(product: AdvertisementContractProductDto): MyAdvertisementContractProductResult =
            MyAdvertisementContractProductResult(
                contractProductId = product.contractProductId,
                productName = product.productName,
                category = product.category,
                position = product.position,
                productDeleted = product.productDeleted,
                startDate = product.startDate,
                endDate = product.endDate,
                reportAvailable = product.reportAvailable,
                reportUnavailableReason = product.reportUnavailableReason,
            )
    }
}
