package com.ogonggo.adminapi.banner.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.banner.domain.Banner
import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.core.banner.domain.BannerSettings
import java.time.LocalDateTime
import java.util.Optional

/** 배너 등록 본문이다. 검증 전 값을 그대로 담는다. `startAt`·`endAt`·`mobileImageUrl`·`advertiserId`·`contractId`는 null을 받는다. */
data class AdminBannerCreateCommand(
    val placement: BannerPlacement?,
    val name: String?,
    val pcImageUrl: String?,
    val mobileImageUrl: String?,
    val linkUrl: String?,
    val openInNewTab: Boolean?,
    val altText: String?,
    val showAdLabel: Boolean?,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val enabled: Boolean?,
    val advertiserId: Long?,
    val contractId: Long?,
) {
    internal fun toDraft(): AdminBannerDraft = AdminBannerDraft(
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
        uploadedImageUrlsToCheck = setOfNotNull(pcImageUrl, mobileImageUrl),
    )
}

/**
 * 배너 수정 본문이다. null이면 저장된 값을 그대로 둔다. null을 보낼 수 있는 칸(`mobileImageUrl`, `startAt`, `endAt`,
 * `advertiserId`, `contractId`)은 보내지 않으면 null, null을 보내면 빈 [Optional]이다. `placement`는 저장된 값과 같을 때만 받는다.
 */
data class AdminBannerUpdateCommand(
    val placement: BannerPlacement? = null,
    val name: String? = null,
    val pcImageUrl: String? = null,
    val mobileImageUrl: Optional<String>? = null,
    val linkUrl: String? = null,
    val openInNewTab: Boolean? = null,
    val altText: String? = null,
    val showAdLabel: Boolean? = null,
    val startAt: Optional<LocalDateTime>? = null,
    val endAt: Optional<LocalDateTime>? = null,
    val enabled: Boolean? = null,
    val advertiserId: Optional<Long>? = null,
    val contractId: Optional<Long>? = null,
) {
    /** 저장된 값 위에 보낸 칸을 얹는다. 검증은 합친 결과로 하고, 오공고에 올린 이미지인지는 새로 보낸 이미지만 본다. */
    internal fun mergeInto(banner: Banner): AdminBannerDraft {
        if (placement != null && placement != banner.placement) {
            throw AdminConsoleBadRequestException.field("placement", "자리는 바꿀 수 없습니다. 새로 등록해 주세요.")
        }
        val mergedMobileImageUrl = if (mobileImageUrl == null) banner.mobileImageUrl else mobileImageUrl.orElse(null)
        return AdminBannerDraft(
            name = name ?: banner.name,
            pcImageUrl = pcImageUrl ?: banner.pcImageUrl,
            mobileImageUrl = mergedMobileImageUrl,
            linkUrl = linkUrl ?: banner.linkUrl,
            openInNewTab = openInNewTab ?: banner.openInNewTab,
            altText = altText ?: banner.altText,
            showAdLabel = showAdLabel ?: banner.showAdLabel,
            startAt = if (startAt == null) banner.startAt else startAt.orElse(null),
            endAt = if (endAt == null) banner.endAt else endAt.orElse(null),
            enabled = enabled ?: banner.enabled,
            advertiserId = if (advertiserId == null) banner.advertiserId else advertiserId.orElse(null),
            contractId = if (contractId == null) banner.contractId else contractId.orElse(null),
            uploadedImageUrlsToCheck = setOfNotNull(pcImageUrl, mobileImageUrl?.orElse(null)),
        )
    }
}

/** 배너가 가리키는 다른 것(올린 이미지, 광고주, 광고 계약)을 확인한다. 서비스가 core 조회로 채운다. */
internal interface AdminBannerReferences {
    /** 오공고에 올렸고 아직 지우지 않은 이미지의 URL인가. */
    fun isUploadedImage(url: String): Boolean

    fun advertiserExists(advertiserId: Long): Boolean

    /** 지우지 않은 계약이면 그 계약의 광고주 식별자, 없으면 null이다. */
    fun contractAdvertiserId(contractId: Long): Long?
}

/**
 * 등록 본문이나 저장된 값과 합친 수정 본문이다. 프런트 목과 같은 순서로 보고 처음 걸린 칸 하나만 알린다:
 * 칸 형식(name → pcImageUrl → mobileImageUrl → linkUrl → altText → 참·거짓 칸) → 기간 앞뒤 → 광고주 → 상세 사이드 모바일 이미지 → 계약.
 */
internal data class AdminBannerDraft(
    val name: String?,
    val pcImageUrl: String?,
    val mobileImageUrl: String?,
    val linkUrl: String?,
    val openInNewTab: Boolean?,
    val altText: String?,
    val showAdLabel: Boolean?,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val enabled: Boolean?,
    val advertiserId: Long?,
    val contractId: Long?,
    /** 오공고에 올린 이미지인지 볼 URL이다. 수정이면 새로 보낸 것만 본다. `/banners/` 기본 소재는 보지 않는다. */
    val uploadedImageUrlsToCheck: Set<String>,
) {
    fun toSettings(placement: BannerPlacement, references: AdminBannerReferences): BannerSettings {
        val name = name?.trim().orEmpty()
        if (name.length !in 1..BannerSettings.MAX_NAME_LENGTH) {
            throw AdminConsoleBadRequestException.field("name", "1~${BannerSettings.MAX_NAME_LENGTH}자로 보내 주세요.")
        }
        val pcImageUrl = imageUrl("pcImageUrl", pcImageUrl, references)
            ?: throw imageUrlError("pcImageUrl")
        val mobileImageUrl = imageUrl("mobileImageUrl", mobileImageUrl, references)
        val linkUrl = linkUrl?.takeIf(BannerSettings::isLinkUrl)
            ?: throw AdminConsoleBadRequestException.field("linkUrl", "http(s):// 주소나 / 로 시작하는 경로를 보내 주세요.")
        val altText = altText?.trim().orEmpty()
        if (altText.length !in 1..BannerSettings.MAX_ALT_TEXT_LENGTH) {
            throw AdminConsoleBadRequestException.field("altText", "1~${BannerSettings.MAX_ALT_TEXT_LENGTH}자로 보내 주세요.")
        }
        val openInNewTab = openInNewTab ?: throw booleanError("openInNewTab")
        val showAdLabel = showAdLabel ?: throw booleanError("showAdLabel")
        val enabled = enabled ?: throw booleanError("enabled")
        if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
            throw AdminConsoleBadRequestException.field("endAt", "노출 끝은 시작보다 뒤여야 합니다.")
        }
        if (advertiserId != null && (advertiserId <= 0 || !references.advertiserExists(advertiserId))) {
            throw AdminConsoleBadRequestException.field("advertiserId", "있는 광고주를 골라 주세요.")
        }
        if (showAdLabel && advertiserId == null) {
            throw AdminConsoleBadRequestException.field("advertiserId", "광고 표시가 켜진 배너는 광고주를 골라 주세요.")
        }
        if (placement.requiresMobileImage && mobileImageUrl == null) {
            throw AdminConsoleBadRequestException.field("mobileImageUrl", "상세 사이드 배너는 모바일 이미지(750×200)가 있어야 합니다.")
        }
        if (contractId != null) {
            val contractAdvertiserId = contractId.takeIf { it > 0 }?.let(references::contractAdvertiserId)
                ?: throw AdminConsoleBadRequestException.field("contractId", "있는 계약을 골라 주세요.")
            if (contractAdvertiserId != advertiserId) {
                throw AdminConsoleBadRequestException.field("contractId", "고른 광고주의 계약만 이을 수 있습니다.")
            }
        }
        return BannerSettings(
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

    /** null은 그대로 null이다. https 주소는 오공고에 올린 이미지여야 하고, `/banners/` 기본 소재 경로는 그대로 받는다. */
    private fun imageUrl(fieldName: String, value: String?, references: AdminBannerReferences): String? {
        if (value == null) return null
        if (!BannerSettings.isImageUrl(value)) throw imageUrlError(fieldName)
        if (value in uploadedImageUrlsToCheck && !value.startsWith(BannerSettings.WEB_ASSET_PATH_PREFIX) && !references.isUploadedImage(value)) {
            throw AdminConsoleBadRequestException.field(fieldName, "오공고에 올린 이미지가 아니거나 지워진 이미지입니다. 다시 올려 주세요.")
        }
        return value
    }

    private fun imageUrlError(fieldName: String) =
        AdminConsoleBadRequestException.field(fieldName, "https 이미지 주소나 /banners/ 경로를 보내 주세요.")

    private fun booleanError(fieldName: String) = AdminConsoleBadRequestException.field(fieldName, "참·거짓 값을 보내 주세요.")
}
