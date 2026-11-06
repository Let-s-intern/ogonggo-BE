package com.ogonggo.adminapi.adcalendar.presentation.response

import com.ogonggo.adminapi.adcalendar.business.AdminAdCalendarContractResult
import com.ogonggo.adminapi.adcalendar.business.AdminAdCalendarResult
import com.ogonggo.adminapi.adposting.presentation.response.AdminAdPostingResponse
import com.ogonggo.adminapi.banner.presentation.response.AdminBannerResponse
import com.ogonggo.adminapi.channelad.presentation.response.AdminChannelAdResponse
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminAdCalendarResponse(
    @Schema(description = "기간이 겹치는 광고 계약. 시작, id 순입니다.")
    val contracts: List<AdminAdCalendarContractResponse>,
    @Schema(description = "노출 기간이 겹치는 배너(꺼진 것 포함). 한 건은 GET /api/v1/admin/banners 항목과 같습니다.")
    val banners: List<AdminBannerResponse>,
    @Schema(description = "노출 기간이 겹치는 광고 공고(꺼진 것 포함). 한 건은 GET /api/v1/admin/ad-postings 항목과 같습니다.")
    val adPostings: List<AdminAdPostingResponse>,
    @Schema(
        description = "게시일~끝 날(없으면 게시일 하루, 둘 다 포함)이 겹치는 채널 광고(모든 플랫폼, 취소한 것 포함). " +
            "한 건은 GET /api/v1/admin/channel-ads 항목과 같습니다.",
    )
    val channelAds: List<AdminChannelAdResponse>,
) {
    companion object {
        internal fun from(result: AdminAdCalendarResult): AdminAdCalendarResponse = AdminAdCalendarResponse(
            contracts = result.contracts.map(AdminAdCalendarContractResponse::from),
            banners = result.banners.map(AdminBannerResponse::from),
            adPostings = result.adPostings.map(AdminAdPostingResponse::from),
            channelAds = result.channelAds.map(AdminChannelAdResponse::from),
        )
    }
}

/** 달력에 그릴 광고 계약이다. 계약 응답의 일부와 광고주 요약이다. */
data class AdminAdCalendarContractResponse(
    val id: Long,
    @Schema(description = "계약명. 없으면 null입니다.")
    val name: String?,
    @Schema(description = "광고 시작(KST, 포함)")
    val startAt: LocalDateTime,
    @Schema(description = "광고 끝(KST, 미포함)")
    val endAt: LocalDateTime,
    val advertiserId: Long,
    @Schema(description = "광고주 요약. 광고주를 찾을 수 없으면 null입니다.")
    val advertiser: AdminAdCalendarAdvertiserResponse?,
    @Schema(description = "금액(원)")
    val amount: Long,
    val paid: Boolean,
    val products: List<AdContractProduct>,
) {
    companion object {
        internal fun from(result: AdminAdCalendarContractResult): AdminAdCalendarContractResponse = AdminAdCalendarContractResponse(
            id = result.id,
            name = result.name,
            startAt = result.startAt,
            endAt = result.endAt,
            advertiserId = result.advertiserId,
            advertiser = result.advertiser?.let { AdminAdCalendarAdvertiserResponse(it.id, it.name, it.type) },
            amount = result.amount,
            paid = result.paid,
            products = result.products,
        )
    }
}

data class AdminAdCalendarAdvertiserResponse(
    val id: Long,
    val name: String,
    val type: AdvertiserType,
)
