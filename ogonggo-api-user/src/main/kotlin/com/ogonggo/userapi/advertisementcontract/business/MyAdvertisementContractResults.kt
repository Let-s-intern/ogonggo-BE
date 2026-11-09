package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractBudgetRange
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
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
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentDeadline: LocalDate,
    val categories: List<AdvertisementProductCategory>,
    val paymentAmount: Int?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(contract: AdvertisementContract): MyAdvertisementContractSummaryResult =
            MyAdvertisementContractSummaryResult(
                id = checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." },
                status = contract.status,
                promotionTarget = contract.promotionTarget,
                companyName = contract.companyName,
                recruitmentTitle = contract.recruitmentTitle,
                recruitmentDeadline = checkNotNull(contract.recruitmentDeadline) { "신청 폼 계약은 recruitmentDeadline 값이 있습니다." },
                categories = contract.categories.sorted(),
                paymentAmount = contract.paymentAmount,
                registeredAt = contract.createdAt,
            )
    }
}

/**
 * 신청자가 낸 값과 운영자가 정한 상태·결제 금액·상품이다. 회원 식별자처럼 신청자에게 필요 없는 값은 싣지 않는다.
 * 신청 폼 계약은 폼의 필수 값을 반드시 가지므로([AdvertisementContract] 생성 규칙) 그 값은 null이 아니다.
 */
data class MyAdvertisementContractDetailResult(
    val id: Long,
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentUrl: String,
    val recruitmentDeadline: LocalDate,
    val purpose: AdvertisementContractPurpose,
    val budgetRange: AdvertisementContractBudgetRange?,
    val categories: List<AdvertisementProductCategory>,
    val scheduleRequest: String?,
    val advertisementRequest: String?,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
    val businessNumberType: CompanyBusinessNumberType?,
    val businessNumber: String?,
    val invoiceMethod: AdvertisementContractInvoiceMethod,
    val creativeMethod: AdvertisementContractCreativeMethod,
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
            status = contract.status,
            promotionTarget = contract.promotionTarget,
            companyName = contract.companyName,
            recruitmentTitle = contract.recruitmentTitle,
            recruitmentUrl = checkNotNull(contract.recruitmentUrl) { "신청 폼 계약은 recruitmentUrl 값이 있습니다." },
            recruitmentDeadline = checkNotNull(contract.recruitmentDeadline) { "신청 폼 계약은 recruitmentDeadline 값이 있습니다." },
            purpose = checkNotNull(contract.purpose) { "신청 폼 계약은 purpose 값이 있습니다." },
            budgetRange = contract.budgetRange,
            categories = contract.categories.sorted(),
            scheduleRequest = contract.scheduleRequest,
            advertisementRequest = contract.advertisementRequest,
            managerName = checkNotNull(contract.managerName) { "신청 폼 계약은 managerName 값이 있습니다." },
            email = checkNotNull(contract.email) { "신청 폼 계약은 email 값이 있습니다." },
            phoneNumber = checkNotNull(contract.phoneNumber) { "신청 폼 계약은 phoneNumber 값이 있습니다." },
            businessNumberType = contract.businessNumberType,
            businessNumber = contract.businessNumber,
            invoiceMethod = checkNotNull(contract.invoiceMethod) { "신청 폼 계약은 invoiceMethod 값이 있습니다." },
            creativeMethod = checkNotNull(contract.creativeMethod) { "신청 폼 계약은 creativeMethod 값이 있습니다." },
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
) {
    companion object {
        internal fun from(product: AdvertisementContractProductDto): MyAdvertisementContractProductResult =
            MyAdvertisementContractProductResult(
                contractProductId = product.contractProductId,
                productName = product.productName,
                category = product.category,
                position = product.position,
                productDeleted = product.productDeleted,
            )
    }
}
