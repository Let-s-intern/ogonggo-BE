package com.ogonggo.core.advertisementcontract.implement.dto

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractFormAnswer
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractProduct
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractReportUnavailableReason
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
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
    val budgetAmount: Int?,
    val categories: Set<AdvertisementProductCategory>,
    val scheduleRequest: String?,
    val advertisementRequest: String?,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
    val businessNumberType: CompanyBusinessNumberType? = null,
    val businessNumber: String? = null,
    val invoiceMethod: AdvertisementContractInvoiceMethod,
    val creativeMethod: AdvertisementContractCreativeMethod,
    val formAnswers: List<AdvertisementContractFormAnswer>? = null,
)

data class AdvertisementContractPageDto(
    val advertisementContracts: List<AdvertisementContract>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/** 계약에 붙인 상품이다. 상품을 지웠어도 이름을 보여 준다. 게시 기간은 기간을 넣기 전에 붙인 상품이면 없다. */
data class AdvertisementContractProductDto(
    val contractProductId: Long,
    val productId: Long,
    val productName: String,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val productDeleted: Boolean,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val addedAt: LocalDateTime,
    val reportUnavailableReason: AdvertisementContractReportUnavailableReason? =
        AdvertisementContractReportUnavailableReason.of(category, startDate, endDate),
) {
    /** 성과 리포트를 열 수 있는 상품인지다. */
    val reportAvailable: Boolean
        get() = reportUnavailableReason == null

    companion object {
        fun of(contractProduct: AdvertisementContractProduct, product: AdvertisementProduct) = AdvertisementContractProductDto(
            contractProductId = checkNotNull(contractProduct.id),
            productId = checkNotNull(product.id),
            productName = product.name,
            category = product.category,
            position = product.position,
            productDeleted = product.deletedAt != null,
            startDate = contractProduct.startDate,
            endDate = contractProduct.endDate,
            addedAt = contractProduct.createdAt,
        )
    }
}

/**
 * 리포트 대상 공고·부트캠프다. 비즈니스 공고 계약의 공고는 자동으로 맨 앞에 오고 뺄 수 없다([fixed]).
 * 제목·회사명·이미지는 조회 시점의 공고 값이며, 지운 공고도 연결이 남아 있으면 보인다.
 */
data class AdvertisementContractTargetDto(
    val targetType: AdvertisementContractPromotionTarget,
    val targetId: Long,
    val title: String,
    val companyName: String,
    val thumbnailUrl: String?,
    val logoUrl: String?,
    val fixed: Boolean,
)
