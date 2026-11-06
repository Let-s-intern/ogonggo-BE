package com.ogonggo.adminapi.advertisercontract.presentation.response

import com.ogonggo.adminapi.advertisercontract.business.AdminAdvertiserContractResult
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminAdvertiserContractResponse(
    val id: Long,
    val advertiserId: Long,
    @Schema(description = "계약명. 없으면 null입니다.")
    val name: String?,
    @Schema(description = "광고 시작(KST, 포함)")
    val startAt: LocalDateTime,
    @Schema(description = "광고 끝(KST, 미포함)")
    val endAt: LocalDateTime,
    @Schema(description = "금액(원)")
    val amount: Long,
    @Schema(description = "amount가 부가가치세 포함 금액인가")
    val vatIncluded: Boolean,
    val paid: Boolean,
    @Schema(description = "결제일(KST). 결제 전이거나 모르면 null입니다.")
    val paidDate: LocalDate?,
    @Schema(description = "산 자리. 선언 순서로 정렬되어 있습니다.")
    val products: List<AdContractProduct>,
    val memo: String?,
    @Schema(description = "이 계약에 이어 둔 배너 수")
    val bannerCount: Int,
    @Schema(description = "이 계약에 이어 둔 광고 공고 수")
    val adPostingCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminAdvertiserContractResult): AdminAdvertiserContractResponse = AdminAdvertiserContractResponse(
            id = result.id,
            advertiserId = result.advertiserId,
            name = result.name,
            startAt = result.startAt,
            endAt = result.endAt,
            amount = result.amount,
            vatIncluded = result.vatIncluded,
            paid = result.paid,
            paidDate = result.paidDate,
            products = result.products,
            memo = result.memo,
            bannerCount = result.bannerCount,
            adPostingCount = result.adPostingCount,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
        )
    }
}

/** 지운 계약의 식별자다. */
data class AdminAdvertiserContractIdResponse(
    val id: Long,
)
