package com.ogonggo.adminapi.adcalendar.business

import com.ogonggo.adminapi.adposting.business.AdminAdPostingResult
import com.ogonggo.adminapi.banner.business.AdminBannerResult
import com.ogonggo.adminapi.channelad.business.AdminChannelAdResult
import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import java.time.LocalDateTime

/** 기간이 겹치는 광고 일정이다. 배너·광고 공고·채널 광고 한 건은 각 관리자 목록의 결과와 같다. */
data class AdminAdCalendarResult(
    val contracts: List<AdminAdCalendarContractResult>,
    val banners: List<AdminBannerResult>,
    val adPostings: List<AdminAdPostingResult>,
    val channelAds: List<AdminChannelAdResult>,
)

/** 달력에 그릴 광고 계약이다. 계약 응답에서 달력이 쓰는 칸과 광고주 요약이다. */
data class AdminAdCalendarContractResult(
    val id: Long,
    val name: String?,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val advertiserId: Long,
    val advertiser: AdminAdCalendarAdvertiserResult?,
    val amount: Long,
    val paid: Boolean,
    val products: List<AdContractProduct>,
) {
    companion object {
        internal fun of(contract: AdvertiserContract, advertiser: Advertiser?): AdminAdCalendarContractResult =
            AdminAdCalendarContractResult(
                id = checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." },
                name = contract.name,
                startAt = contract.startAt,
                endAt = contract.endAt,
                advertiserId = contract.advertiserId,
                advertiser = advertiser?.let { AdminAdCalendarAdvertiserResult(id = checkNotNull(it.id), name = it.name, type = it.type) },
                amount = contract.amount,
                paid = contract.paid,
                products = contract.products,
            )
    }
}

data class AdminAdCalendarAdvertiserResult(
    val id: Long,
    val name: String,
    val type: AdvertiserType,
)
