package com.ogonggo.core.advertisementcontract.implement.dto

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import java.time.LocalDate
import java.time.LocalDateTime

/** 기업회원이 비즈니스로 등록한 공고로 계약을 만들 때 넣는 값이다. */
data class AdvertisementContractCompanyPostingAppendDto(
    val userId: Long,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val targetId: Long,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentDeadline: LocalDate?,
)

/** 광고 신청 폼이 받은 값이다. 비회원이면 회원 식별자가 없다. */
data class AdvertisementContractFormAppendDto(
    val userId: Long?,
    val promotionTarget: AdvertisementContractPromotionTarget,
    val companyName: String,
    val recruitmentTitle: String,
    val recruitmentUrl: String,
    val recruitmentDeadline: LocalDate,
    val purpose: AdvertisementContractPurpose,
    val categories: Set<AdvertisementProductCategory>,
    val scheduleRequest: String?,
    val advertisementRequest: String?,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
    val invoiceMethod: AdvertisementContractInvoiceMethod,
    val creativeMethod: AdvertisementContractCreativeMethod,
)

data class AdvertisementContractPageDto(
    val advertisementContracts: List<AdvertisementContract>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/** 계약에 붙인 상품이다. 상품을 지웠어도 이름을 보여 준다. */
data class AdvertisementContractProductDto(
    val contractProductId: Long,
    val productId: Long,
    val productName: String,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val productDeleted: Boolean,
    val addedAt: LocalDateTime,
) {
    companion object {
        fun of(contractProductId: Long, product: AdvertisementProduct, addedAt: LocalDateTime) = AdvertisementContractProductDto(
            contractProductId = contractProductId,
            productId = checkNotNull(product.id),
            productName = product.name,
            category = product.category,
            position = product.position,
            productDeleted = product.deletedAt != null,
            addedAt = addedAt,
        )
    }
}
