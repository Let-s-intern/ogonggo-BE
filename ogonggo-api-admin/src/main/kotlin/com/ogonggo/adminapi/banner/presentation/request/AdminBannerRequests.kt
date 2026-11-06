package com.ogonggo.adminapi.banner.presentation.request

import com.ogonggo.adminapi.banner.business.AdminBannerCreateCommand
import com.ogonggo.adminapi.banner.business.AdminBannerUpdateCommand
import com.ogonggo.core.banner.domain.BannerPlacement
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.Optional

/** 칸 검증은 화면 문구(`필드: 설명`)로 알리려고 서비스가 한다. */
data class CreateAdminBannerRequest(
    @Schema(description = "자리. 등록 뒤 바꾸지 않습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val placement: BannerPlacement? = null,
    @Schema(description = "운영자용 이름. 앞뒤 공백을 빼고 1~50자. 사용자에게 보이지 않습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String? = null,
    @Schema(
        description = "PC 이미지. 오공고에 올린 이미지의 https 주소(`POST /api/v1/images?usage=BANNER`의 url)이거나 사용자 웹의 `/banners/` 경로",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val pcImageUrl: String? = null,
    @Schema(
        description = "모바일 이미지(750×200). pcImageUrl과 같은 규칙. 목록 위·아래 자리는 null을 받고, 상세 사이드 자리는 꼭 있어야 합니다.",
        nullable = true,
    )
    val mobileImageUrl: String? = null,
    @Schema(description = "http(s):// 주소나 / 로 시작하는 사이트 안 경로(// 로 시작하면 거절). 2048자 이하", requiredMode = Schema.RequiredMode.REQUIRED)
    val linkUrl: String? = null,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val openInNewTab: Boolean? = null,
    @Schema(description = "이미지 대체 텍스트. 앞뒤 공백을 빼고 1~100자", requiredMode = Schema.RequiredMode.REQUIRED)
    val altText: String? = null,
    @Schema(description = "웹이 `광고` 표시를 붙일지. true면 advertiserId가 있어야 합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val showAdLabel: Boolean? = null,
    @Schema(description = "노출 시작(KST, 포함). null이면 켜는 즉시", nullable = true)
    val startAt: LocalDateTime? = null,
    @Schema(description = "노출 끝(KST, 미포함). null이면 끌 때까지. startAt보다 뒤여야 합니다.", nullable = true)
    val endAt: LocalDateTime? = null,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val enabled: Boolean? = null,
    @Schema(description = "광고주. showAdLabel이 true면 필수", nullable = true)
    val advertiserId: Long? = null,
    @Schema(description = "광고 계약. advertiserId의 계약만 받습니다. 노출 기간이 계약 기간 밖이어도 받습니다.", nullable = true)
    val contractId: Long? = null,
) {
    fun toCommand(): AdminBannerCreateCommand = AdminBannerCreateCommand(
        placement = placement,
        name = name,
        pcImageUrl = pcImageUrl,
        mobileImageUrl = mobileImageUrl,
        linkUrl = linkUrl,
        openInNewTab = openInNewTab,
        altText = altText,
        showAdLabel = showAdLabel,
        startAt = startAt,
        endAt = endAt,
        enabled = enabled,
        advertiserId = advertiserId,
        contractId = contractId,
    )
}

/**
 * 보낸 칸만 바꿉니다. 검증은 저장된 값과 합친 결과로 합니다. `mobileImageUrl`, `startAt`, `endAt`, `advertiserId`, `contractId`는
 * null을 보내면 비웁니다. 그 밖의 칸은 null이면 보내지 않은 것과 같습니다. `placement`는 저장된 값과 같을 때만 받습니다.
 */
data class UpdateAdminBannerRequest(
    @Schema(description = "보내면 저장된 자리와 같아야 합니다.")
    val placement: BannerPlacement? = null,
    val name: String? = null,
    val pcImageUrl: String? = null,
    @Schema(implementation = String::class, nullable = true, description = "null을 보내면 모바일 이미지를 비웁니다.")
    val mobileImageUrl: Optional<String>? = null,
    val linkUrl: String? = null,
    val openInNewTab: Boolean? = null,
    val altText: String? = null,
    val showAdLabel: Boolean? = null,
    @Schema(implementation = LocalDateTime::class, nullable = true, description = "null을 보내면 시작을 비웁니다.")
    val startAt: Optional<LocalDateTime>? = null,
    @Schema(implementation = LocalDateTime::class, nullable = true, description = "null을 보내면 끝을 비웁니다.")
    val endAt: Optional<LocalDateTime>? = null,
    val enabled: Boolean? = null,
    @Schema(implementation = Long::class, nullable = true, description = "null을 보내면 광고주를 비웁니다.")
    val advertiserId: Optional<Long>? = null,
    @Schema(implementation = Long::class, nullable = true, description = "null을 보내면 계약 연결을 끊습니다.")
    val contractId: Optional<Long>? = null,
) {
    fun toCommand(): AdminBannerUpdateCommand = AdminBannerUpdateCommand(
        placement = placement,
        name = name,
        pcImageUrl = pcImageUrl,
        mobileImageUrl = mobileImageUrl,
        linkUrl = linkUrl,
        openInNewTab = openInNewTab,
        altText = altText,
        showAdLabel = showAdLabel,
        startAt = startAt,
        endAt = endAt,
        enabled = enabled,
        advertiserId = advertiserId,
        contractId = contractId,
    )
}

/** 자리 안 순서를 통째로 바꿉니다. 그 자리의 배너 id 를 빠짐없이 한 번씩 새 순서대로 보냅니다. */
data class ReplaceAdminBannerOrderRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val placement: BannerPlacement? = null,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val bannerIds: List<Long?>? = null,
)
