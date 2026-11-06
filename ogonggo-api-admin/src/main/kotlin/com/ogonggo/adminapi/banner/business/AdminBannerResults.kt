package com.ogonggo.adminapi.banner.business

import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.banner.domain.Banner
import com.ogonggo.core.banner.domain.BannerPlacement
import java.time.LocalDateTime

data class AdminBannerResult(
    val id: Long,
    val placement: BannerPlacement,
    val name: String,
    val pcImageUrl: String,
    val mobileImageUrl: String?,
    val linkUrl: String,
    val openInNewTab: Boolean,
    val altText: String,
    val showAdLabel: Boolean,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val enabled: Boolean,
    val displayOrder: Int,
    val advertiserId: Long?,
    val advertiser: AdminBannerAdvertiserResult?,
    val contractId: Long?,
    val contract: AdminBannerContractResult?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun of(banner: Banner, advertiser: Advertiser?, contract: AdvertiserContract?): AdminBannerResult =
            AdminBannerResult(
                id = banner.requiredId(),
                placement = banner.placement,
                name = banner.name,
                pcImageUrl = banner.pcImageUrl,
                mobileImageUrl = banner.mobileImageUrl,
                linkUrl = banner.linkUrl,
                openInNewTab = banner.openInNewTab,
                altText = banner.altText,
                showAdLabel = banner.showAdLabel,
                startAt = banner.startAt,
                endAt = banner.endAt,
                enabled = banner.enabled,
                displayOrder = banner.displayOrder,
                advertiserId = banner.advertiserId,
                advertiser = advertiser?.let {
                    AdminBannerAdvertiserResult(id = checkNotNull(it.id), name = it.name, type = it.type)
                },
                contractId = banner.contractId,
                contract = contract?.let {
                    AdminBannerContractResult(id = checkNotNull(it.id), name = it.name, startAt = it.startAt, endAt = it.endAt)
                },
                createdAt = banner.createdAt,
                updatedAt = banner.updatedAt,
            )
    }
}

/** 표에 보일 광고주 요약이다. */
data class AdminBannerAdvertiserResult(
    val id: Long,
    val name: String,
    val type: AdvertiserType,
)

/** 표와 기간 밖 경고에 쓸 광고 계약 요약이다. */
data class AdminBannerContractResult(
    val id: Long,
    val name: String?,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
)

internal fun Banner.requiredId(): Long = checkNotNull(id) { "배너 식별자가 없습니다." }
