package com.ogonggo.adminapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractPageDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractProductDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminAdvertisementContractPageResult(
    val items: List<AdminAdvertisementContractSummaryResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(page: AdvertisementContractPageDto): AdminAdvertisementContractPageResult =
            AdminAdvertisementContractPageResult(
                items = page.advertisementContracts.map(AdminAdvertisementContractSummaryResult::from),
                page = page.page,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
            )
    }
}

data class AdminAdvertisementContractSummaryResult(
    val id: Long,
    val source: AdvertisementContractSource,
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val targetId: Long?,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentDeadline: LocalDate?,
    val budgetAmount: Int?,
    val categories: List<AdvertisementProductCategory>,
    val paymentAmount: Int?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(contract: AdvertisementContract): AdminAdvertisementContractSummaryResult =
            AdminAdvertisementContractSummaryResult(
                id = checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." },
                source = contract.source,
                status = contract.status,
                promotionTarget = contract.promotionTarget,
                targetId = contract.targetId,
                companyName = contract.companyName,
                recruitmentTitle = contract.recruitmentTitle,
                recruitmentDeadline = contract.recruitmentDeadline,
                budgetAmount = contract.budgetAmount,
                categories = contract.categories.sorted(),
                paymentAmount = contract.paymentAmount,
                registeredAt = contract.createdAt,
                updatedAt = contract.updatedAt,
            )
    }
}

data class AdminAdvertisementContractDetailResult(
    val id: Long,
    val source: AdvertisementContractSource,
    val status: AdvertisementContractStatus,
    val userId: Long?,
    val targetId: Long?,
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
    val paymentAmount: Int?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val products: List<AdminAdvertisementContractProductResult>,
) {
    companion object {
        internal fun from(
            contract: AdvertisementContract,
            products: List<AdvertisementContractProductDto>,
        ): AdminAdvertisementContractDetailResult = AdminAdvertisementContractDetailResult(
            id = checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." },
            source = contract.source,
            status = contract.status,
            userId = contract.userId,
            targetId = contract.targetId,
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
            paymentAmount = contract.paymentAmount,
            registeredAt = contract.createdAt,
            updatedAt = contract.updatedAt,
            products = products.map(AdminAdvertisementContractProductResult::from),
        )
    }
}

data class AdminAdvertisementContractProductResult(
    val contractProductId: Long,
    val productId: Long,
    val productName: String,
    val category: AdvertisementProductCategory,
    val productDeleted: Boolean,
    val addedAt: LocalDateTime,
) {
    companion object {
        internal fun from(product: AdvertisementContractProductDto): AdminAdvertisementContractProductResult =
            AdminAdvertisementContractProductResult(
                contractProductId = product.contractProductId,
                productId = product.productId,
                productName = product.productName,
                category = product.category,
                productDeleted = product.productDeleted,
                addedAt = product.addedAt,
            )
    }
}
