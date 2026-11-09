package com.ogonggo.adminapi.advertisementcontract.presentation.response

import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractDetailResult
import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractProductResult
import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementContractSummaryResult
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractBudgetRange
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminAdvertisementContractSummaryResponse(
    val id: Long,
    val source: AdvertisementContractSource,
    val status: AdvertisementContractStatus,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val targetId: Long?,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentDeadline: LocalDate?,
    val budgetRange: AdvertisementContractBudgetRange?,
    val categories: List<AdvertisementProductCategory>,
    val paymentAmount: Int?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(result: AdminAdvertisementContractSummaryResult) = AdminAdvertisementContractSummaryResponse(
            id = result.id,
            source = result.source,
            status = result.status,
            promotionTarget = result.promotionTarget,
            targetId = result.targetId,
            companyName = result.companyName,
            recruitmentTitle = result.recruitmentTitle,
            recruitmentDeadline = result.recruitmentDeadline,
            budgetRange = result.budgetRange,
            categories = result.categories,
            paymentAmount = result.paymentAmount,
            registeredAt = result.registeredAt,
            updatedAt = result.updatedAt,
        )
    }
}

data class AdminAdvertisementContractDetailResponse(
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
    val budgetRange: AdvertisementContractBudgetRange?,
    val categories: List<AdvertisementProductCategory>,
    val scheduleRequest: String?,
    val advertisementRequest: String?,
    val managerName: String?,
    val email: String?,
    val phoneNumber: String?,
    val invoiceMethod: AdvertisementContractInvoiceMethod?,
    val creativeMethod: AdvertisementContractCreativeMethod?,
    val paymentAmount: Int?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val products: List<AdminAdvertisementContractProductResponse>,
) {
    companion object {
        fun from(result: AdminAdvertisementContractDetailResult) = AdminAdvertisementContractDetailResponse(
            id = result.id,
            source = result.source,
            status = result.status,
            userId = result.userId,
            targetId = result.targetId,
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
            invoiceMethod = result.invoiceMethod,
            creativeMethod = result.creativeMethod,
            paymentAmount = result.paymentAmount,
            registeredAt = result.registeredAt,
            updatedAt = result.updatedAt,
            products = result.products.map(AdminAdvertisementContractProductResponse::from),
        )
    }
}

data class AdminAdvertisementContractProductResponse(
    val contractProductId: Long,
    val productId: Long,
    val productName: String,
    val category: AdvertisementProductCategory,
    val productDeleted: Boolean,
    val addedAt: LocalDateTime,
) {
    companion object {
        fun from(result: AdminAdvertisementContractProductResult) = AdminAdvertisementContractProductResponse(
            contractProductId = result.contractProductId,
            productId = result.productId,
            productName = result.productName,
            category = result.category,
            productDeleted = result.productDeleted,
            addedAt = result.addedAt,
        )
    }
}

data class CreateAdminAdvertisementContractProductResponse(
    val id: Long,
)
