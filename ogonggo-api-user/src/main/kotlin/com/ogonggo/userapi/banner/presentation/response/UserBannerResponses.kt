package com.ogonggo.userapi.banner.presentation.response

import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.userapi.banner.business.UserBannerResult
import io.swagger.v3.oas.annotations.media.Schema

data class UserBannerResponse(
    val id: Long,
    val placement: BannerPlacement,
    @Schema(description = "PC 이미지. 절대 주소이거나 사용자 웹의 `/banners/` 경로")
    val pcImageUrl: String,
    @Schema(description = "모바일 이미지. 없으면 null이고 웹이 PC 이미지를 씁니다.")
    val mobileImageUrl: String?,
    @Schema(description = "http(s):// 주소이거나 / 로 시작하는 사이트 안 경로")
    val linkUrl: String,
    val openInNewTab: Boolean,
    val altText: String,
    @Schema(description = "웹이 `광고` 표시를 붙일지")
    val showAdLabel: Boolean,
) {
    companion object {
        internal fun from(result: UserBannerResult): UserBannerResponse = UserBannerResponse(
            id = result.id,
            placement = result.placement,
            pcImageUrl = result.pcImageUrl,
            mobileImageUrl = result.mobileImageUrl,
            linkUrl = result.linkUrl,
            openInNewTab = result.openInNewTab,
            altText = result.altText,
            showAdLabel = result.showAdLabel,
        )
    }
}
