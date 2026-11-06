package com.ogonggo.core.banner.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.net.URI
import java.time.LocalDateTime

/**
 * 운영자가 사용자 웹의 한 자리([BannerPlacement])에 거는 이미지 배너다. 한 자리에 여럿이면 순서(`displayOrder`, 1부터)대로 웹이 넘긴다.
 *
 * 노출 중은 켜져 있고 기간 안(시작 포함·끝 미포함, 비어 있으면 그쪽이 열려 있음)인 것이다. 상태 칼럼은 없다.
 * 자리는 등록 뒤 바꾸지 않는다. 광고 표시가 켜진 배너는 광고주가 있어야 하고, 광고 계약은 그 광고주의 것만 잇는다.
 * 계약이 지워지면 계약만 끊고 배너는 남긴다. 이미지는 식별자가 아니라 URL로 가지므로 이미지 정리 작업이 이 URL을 확인한다.
 */
@Entity
@Table(
    name = "banners",
    indexes = [
        Index(name = "idx_banners_placement", columnList = "placement, deleted_at, display_order"),
        Index(name = "idx_banners_advertiser_id", columnList = "advertiser_id"),
        Index(name = "idx_banners_contract_id", columnList = "contract_id"),
    ],
)
class Banner internal constructor(
    placement: BannerPlacement,
    settings: BannerSettings,
    displayOrder: Int,
) : BaseTimeEntity() {

    init {
        settings.validateFor(placement)
        require(displayOrder > 0) { "순서는 1부터입니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 배너 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    val placement: BannerPlacement = placement /* 자리. 바꾸지 않는다 */

    @Column(nullable = false, length = BannerSettings.MAX_NAME_LENGTH)
    var name: String = settings.name /* 운영자용 이름 */
        protected set

    @Column(name = "pc_image_url", nullable = false, length = BannerSettings.MAX_URL_LENGTH)
    var pcImageUrl: String = settings.pcImageUrl /* PC 이미지 URL */
        protected set

    @Column(name = "mobile_image_url", length = BannerSettings.MAX_URL_LENGTH)
    var mobileImageUrl: String? = settings.mobileImageUrl /* 모바일 이미지 URL */
        protected set

    @Column(name = "link_url", nullable = false, length = BannerSettings.MAX_URL_LENGTH)
    var linkUrl: String = settings.linkUrl /* 누르면 가는 곳 */
        protected set

    @Column(name = "open_in_new_tab", nullable = false)
    var openInNewTab: Boolean = settings.openInNewTab /* 새 탭으로 열지 */
        protected set

    @Column(name = "alt_text", nullable = false, length = BannerSettings.MAX_ALT_TEXT_LENGTH)
    var altText: String = settings.altText /* 이미지 대체 텍스트 */
        protected set

    @Column(name = "show_ad_label", nullable = false)
    var showAdLabel: Boolean = settings.showAdLabel /* 광고 표시를 붙일지 */
        protected set

    @Column(name = "start_at")
    var startAt: LocalDateTime? = settings.startAt /* 노출 시작(포함). 없으면 켜는 즉시 */
        protected set

    @Column(name = "end_at")
    var endAt: LocalDateTime? = settings.endAt /* 노출 끝(미포함). 없으면 끌 때까지 */
        protected set

    @Column(nullable = false)
    var enabled: Boolean = settings.enabled /* 켜기 */
        protected set

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder /* 자리 안 순서. 1부터 */
        protected set

    @Column(name = "advertiser_id")
    var advertiserId: Long? = settings.advertiserId /* 광고주 식별자 */
        protected set

    @Column(name = "contract_id")
    var contractId: Long? = settings.contractId /* 광고 계약 식별자 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    val settings: BannerSettings
        get() = BannerSettings(
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

    /** 자리 말고 모두 바꾼다. 부분 수정은 저장된 값과 합친 [settings]를 넘긴다. */
    fun change(settings: BannerSettings) {
        check(deletedAt == null) { "삭제된 배너는 수정할 수 없습니다." }
        settings.validateFor(placement)
        name = settings.name
        pcImageUrl = settings.pcImageUrl
        mobileImageUrl = settings.mobileImageUrl
        linkUrl = settings.linkUrl
        openInNewTab = settings.openInNewTab
        altText = settings.altText
        showAdLabel = settings.showAdLabel
        startAt = settings.startAt
        endAt = settings.endAt
        enabled = settings.enabled
        advertiserId = settings.advertiserId
        contractId = settings.contractId
    }

    fun changeDisplayOrder(displayOrder: Int) {
        require(displayOrder > 0) { "순서는 1부터입니다." }
        this.displayOrder = displayOrder
    }

    /** 이어 둔 광고 계약이 지워져 연결만 끊는다. 지운 배너도 끊는다. */
    fun unlinkContract() {
        contractId = null
    }

    /** 지금 노출 중인가. 켜져 있고 시작은 포함, 끝은 포함하지 않는다. */
    fun isLive(now: LocalDateTime): Boolean =
        deletedAt == null && enabled && (startAt?.let { !it.isAfter(now) } ?: true) && (endAt?.let { now.isBefore(it) } ?: true)

    /** 삭제는 반복해도 최초 삭제 일시를 유지한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}

/**
 * 배너에서 자리와 순서를 뺀 내용이다. 이름·대체 텍스트는 앞뒤 공백을 뺀다.
 * 자리에 따라 달라지는 규칙(상세 사이드 자리의 모바일 이미지)은 [validateFor]가 본다.
 */
class BannerSettings(
    name: String,
    val pcImageUrl: String,
    val mobileImageUrl: String?,
    val linkUrl: String,
    val openInNewTab: Boolean,
    altText: String,
    val showAdLabel: Boolean,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val enabled: Boolean,
    val advertiserId: Long?,
    val contractId: Long?,
) {
    val name: String = name.trim()
    val altText: String = altText.trim()

    init {
        require(this.name.length in 1..MAX_NAME_LENGTH) { "이름은 1~${MAX_NAME_LENGTH}자여야 합니다." }
        require(isImageUrl(pcImageUrl)) { "PC 이미지는 https 주소나 /banners/ 경로여야 합니다." }
        require(mobileImageUrl == null || isImageUrl(mobileImageUrl)) { "모바일 이미지는 https 주소나 /banners/ 경로여야 합니다." }
        require(isLinkUrl(linkUrl)) { "링크는 http(s) 주소나 / 로 시작하는 경로여야 합니다." }
        require(this.altText.length in 1..MAX_ALT_TEXT_LENGTH) { "대체 텍스트는 1~${MAX_ALT_TEXT_LENGTH}자여야 합니다." }
        require(startAt == null || endAt == null || startAt.isBefore(endAt)) { "노출 끝은 시작보다 뒤여야 합니다." }
        require(!showAdLabel || advertiserId != null) { "광고 표시가 켜진 배너는 광고주가 있어야 합니다." }
        require(advertiserId == null || advertiserId > 0) { "광고주 식별자는 양수여야 합니다." }
        require(contractId == null || contractId > 0) { "광고 계약 식별자는 양수여야 합니다." }
    }

    fun validateFor(placement: BannerPlacement) {
        require(!placement.requiresMobileImage || mobileImageUrl != null) { "상세 사이드 배너는 모바일 이미지가 있어야 합니다." }
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_ALT_TEXT_LENGTH = 100
        const val MAX_URL_LENGTH = 2048

        /** 사용자 웹이 같은 오리진에 두는 기본 소재 경로다. */
        const val WEB_ASSET_PATH_PREFIX = "/banners/"

        /** https 주소이거나 사용자 웹의 기본 소재 경로(`/banners/...`)인가. 오공고에 올린 이미지인지는 보지 않는다. */
        fun isImageUrl(value: String): Boolean =
            value.length <= MAX_URL_LENGTH &&
                ((value.startsWith(WEB_ASSET_PATH_PREFIX) && !value.contains("..")) || isAbsoluteUrl(value, setOf("https")))

        /** `http(s)://` 주소이거나 `/` 로 시작하는 사이트 안 경로인가. `//` 로 시작하면 다른 호스트라 받지 않는다. */
        fun isLinkUrl(value: String): Boolean =
            value.length <= MAX_URL_LENGTH &&
                (if (value.startsWith("/")) !value.startsWith("//") else isAbsoluteUrl(value, setOf("http", "https")))

        private fun isAbsoluteUrl(value: String, schemes: Set<String>): Boolean {
            val uri = runCatching { URI(value) }.getOrNull() ?: return false
            return uri.scheme?.lowercase() in schemes && !uri.host.isNullOrBlank()
        }
    }
}
