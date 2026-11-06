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
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun of(contract: AdvertiserContract, adCount: AdvertiserContractAdCountDto): AdminAdvertiserContractResult =
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
                createdAt = contract.createdAt,
                updatedAt = contract.updatedAt,
            )
    }
}

internal fun AdvertiserContract.requiredId(): Long = checkNotNull(id) { "광고 계약 식별자가 없습니다." }
