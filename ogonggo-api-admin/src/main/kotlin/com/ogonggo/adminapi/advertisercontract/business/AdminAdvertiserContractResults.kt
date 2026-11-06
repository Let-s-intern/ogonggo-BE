package com.ogonggo.adminapi.advertisercontract.business

import com.ogonggo.core.advertiser.implement.dto.AdvertiserContractAdCountDto
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminAdvertiserContractResult(
    val id: Long,
    val advertiserId: Long,
    val name: String?,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val amount: Long,
    val vatIncluded: Boolean,
    val paid: Boolean,
    val paidDate: LocalDate?,
    val products: List<AdContractProduct>,
    val memo: String?,
    val bannerCount: Int,
    val adPostingCount: Int,
    val channelFormatIds: List<Long> = emptyList(),
    val channelAdCount: Int = 0,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /** [channelFormatOrder]는 유형 목록 순서의 유형 식별자다. 산 유형을 이 순서로 두고 지운 유형은 뺀다. */
        internal fun of(
            contract: AdvertiserContract,
            adCount: AdvertiserContractAdCountDto,
            channelFormatOrder: List<Long>,
            channelAdCount: Int,
        ): AdminAdvertiserContractResult =
            AdminAdvertiserContractResult(
                id = contract.requiredId(),
                advertiserId = contract.advertiserId,
                name = contract.name,
                startAt = contract.startAt,
                endAt = contract.endAt,
                amount = contract.amount,
                vatIncluded = contract.vatIncluded,
                paid = contract.paid,
                paidDate = contract.paidDate,
                products = contract.products,
                memo = contract.memo,
                bannerCount = adCount.bannerCount,
                adPostingCount = adCount.adPostingCount,
                channelFormatIds = channelFormatOrder.filter { it in contract.channelFormatIds },
                channelAdCount = channelAdCount,
                createdAt = contract.createdAt,
                updatedAt = contract.updatedAt,
            )
    }
}

internal fun AdvertiserContract.requiredId(): Long = checkNotNull(id) { "광고 계약 식별자가 없습니다." }
