package com.ogonggo.adminapi.banner.presentation.response

import com.ogonggo.adminapi.banner.business.AdminBannerAdvertiserResult
import com.ogonggo.adminapi.banner.business.AdminBannerContractResult
import com.ogonggo.adminapi.banner.business.AdminBannerResult
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.banner.domain.BannerPlacement
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminBannerResponse(
    val id: Long,
    val placement: BannerPlacement,
    @Schema(description = "운영자용 이름")
    val name: String,
    val pcImageUrl: String,
    @Schema(description = "모바일 이미지. 없으면 null이고 웹이 PC 이미지를 씁니다.")
    val mobileImageUrl: String?,
    val linkUrl: String,
    val openInNewTab: Boolean,
    val altText: String,
    val showAdLabel: Boolean,
    @Schema(description = "노출 시작(KST, 포함). null이면 켜는 즉시")
    val startAt: LocalDateTime?,
    @Schema(description = "노출 끝(KST, 미포함). null이면 끌 때까지")
    val endAt: LocalDateTime?,
    val enabled: Boolean,
    @Schema(description = "자리 안 순서. 1부터, 작은 것이 먼저")
    val displayOrder: Int,
    val advertiserId: Long?,
    @Schema(description = "광고주 요약. 광고주가 없으면 null")
    val advertiser: AdminBannerAdvertiserResponse?,
    val contractId: Long?,
    @Schema(description = "광고 계약 요약. 계약이 없으면 null")
    val contract: AdminBannerContractResponse?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminBannerResult): AdminBannerResponse = AdminBannerResponse(
            id = result.id,
            placement = result.placement,
            name = result.name,
            pcImageUrl = result.pcImageUrl,
            mobileImageUrl = result.mobileImageUrl,
            linkUrl = result.linkUrl,
            openInNewTab = result.openInNewTab,
            altText = result.altText,
            showAdLabel = result.showAdLabel,
            startAt = result.startAt,
            endAt = result.endAt,
            enabled = result.enabled,
            displayOrder = result.displayOrder,
            advertiserId = result.advertiserId,
            advertiser = result.advertiser?.let(AdminBannerAdvertiserResponse::from),
            contractId = result.contractId,
            contract = result.contract?.let(AdminBannerContractResponse::from),
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
        )
    }
}

data class AdminBannerAdvertiserResponse(
    val id: Long,
    val name: String,
    val type: AdvertiserType,
) {
    companion object {
        internal fun from(result: AdminBannerAdvertiserResult) = AdminBannerAdvertiserResponse(result.id, result.name, result.type)
    }
}

data class AdminBannerContractResponse(
    val id: Long,
    @Schema(description = "계약명. 없으면 null")
    val name: String?,
    @Schema(description = "광고 시작(KST, 포함)")
    val startAt: LocalDateTime,
    @Schema(description = "광고 끝(KST, 미포함)")
    val endAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminBannerContractResult) =
            AdminBannerContractResponse(result.id, result.name, result.startAt, result.endAt)
    }
}

/** 지운 배너의 식별자다. */
data class AdminBannerIdResponse(
    val id: Long,
)
