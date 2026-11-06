package com.ogonggo.userapi.banner.business

import com.ogonggo.core.banner.domain.Banner
import com.ogonggo.core.banner.domain.BannerPlacement

/** 사용자 웹에 나가는 배너 한 건이다. 운영자용 이름·기간·켜기·순서는 싣지 않는다. */
data class UserBannerResult(
    val id: Long,
    val placement: BannerPlacement,
    val pcImageUrl: String,
    val mobileImageUrl: String?,
    val linkUrl: String,
    val openInNewTab: Boolean,
    val altText: String,
    val showAdLabel: Boolean,
) {
    companion object {
        internal fun from(banner: Banner): UserBannerResult = UserBannerResult(
            id = checkNotNull(banner.id) { "배너 식별자가 없습니다." },
            placement = banner.placement,
            pcImageUrl = banner.pcImageUrl,
            mobileImageUrl = banner.mobileImageUrl,
            linkUrl = banner.linkUrl,
            openInNewTab = banner.openInNewTab,
            altText = banner.altText,
            showAdLabel = banner.showAdLabel,
        )
    }
}
