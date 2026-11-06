package com.ogonggo.core.channelad.domain

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 광고주의 홍보를 우리 채널에 올리는 일정 한 건이다. 채널 하나에 게재 유형 하나로 올린다.
 *
 * 유형은 채널과 같은 플랫폼이고, 플랫폼(`platform`)은 채널의 것을 함께 적어 둔다(채널의 플랫폼은 바뀌지 않는다).
 * 게시일(`postDate`)부터 노출 끝 날(`endDate`, 그날 포함)까지 차지하며 끝 날이 없으면 하루짜리다. 상태는 운영자가 바꾼다.
 * 계약(`contractId`)은 같은 광고주의 것만 잇고, 계약을 지우면 연결만 끊는다.
 */
@Entity
@Table(
    name = "channel_ads",
    indexes = [
        Index(name = "idx_channel_ads_post_date", columnList = "deleted_at, post_date"),
        Index(name = "idx_channel_ads_advertiser_id", columnList = "advertiser_id"),
        Index(name = "idx_channel_ads_contract_id", columnList = "contract_id"),
        Index(name = "idx_channel_ads_channel_id", columnList = "channel_id"),
        Index(name = "idx_channel_ads_format_id", columnList = "format_id"),
    ],
)
class ChannelAd internal constructor(
    terms: ChannelAdTerms,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 채널 광고 식별자 */
        protected set

    @Column(name = "advertiser_id", nullable = false)
    var advertiserId: Long = terms.advertiserId /* 광고주 식별자 */
        protected set

    @Column(name = "contract_id")
    var contractId: Long? = terms.contractId /* 광고 계약 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var platform: AdChannelPlatform = terms.platform /* 채널의 플랫폼 */
        protected set

    @Column(name = "channel_id", nullable = false)
    var channelId: Long = terms.channelId /* 채널 식별자 */
        protected set

    @Column(name = "format_id", nullable = false)
    var formatId: Long = terms.formatId /* 게재 유형 식별자 */
        protected set

    @Column(name = "post_date", nullable = false)
    var postDate: LocalDate = terms.postDate /* 게시 예정일. 게시 뒤에는 실제 게시일 */
        protected set

    @Column(name = "end_date")
    var endDate: LocalDate? = terms.endDate /* 노출 끝 날(포함). 기간이 있는 유형만 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ChannelAdStatus = terms.status /* 진행 상태 */
        protected set

    @Column(name = "post_url", length = ChannelAdTerms.MAX_URL_LENGTH)
    var postUrl: String? = terms.postUrl /* 게시 확인 주소 */
        protected set

    @Column(name = "creative_memo", length = ChannelAdTerms.MAX_MEMO_LENGTH)
    var creativeMemo: String? = terms.creativeMemo /* 소재·문구 메모 */
        protected set

    @Column(name = "admin_memo", length = ChannelAdTerms.MAX_MEMO_LENGTH)
    var adminMemo: String? = terms.adminMemo /* 담당자 메모 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    val terms: ChannelAdTerms
        get() = ChannelAdTerms(
            advertiserId = advertiserId,
            contractId = contractId,
            platform = platform,
            channelId = channelId,
            formatId = formatId,
            postDate = postDate,
            endDate = endDate,
            status = status,
            postUrl = postUrl,
            creativeMemo = creativeMemo,
            adminMemo = adminMemo,
        )

    /** 칸을 통째로 바꾼다. 부분 수정은 저장된 값과 합친 [terms]를 넘긴다. */
    fun change(terms: ChannelAdTerms) {
        check(deletedAt == null) { "삭제된 채널 광고는 수정할 수 없습니다." }
        advertiserId = terms.advertiserId
        contractId = terms.contractId
        platform = terms.platform
        channelId = terms.channelId
        formatId = terms.formatId
        postDate = terms.postDate
        endDate = terms.endDate
        status = terms.status
        postUrl = terms.postUrl
        creativeMemo = terms.creativeMemo
        adminMemo = terms.adminMemo
    }

    /** 삭제는 반복해도 최초 삭제 일시를 유지한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}

/** 채널 광고 칸이다. 주소·메모는 앞뒤 공백을 빼고 비면 null로 둔다. 채널·유형의 짝과 계약의 광고주는 관리자 API가 본다. */
class ChannelAdTerms(
    val advertiserId: Long,
    val contractId: Long?,
    val platform: AdChannelPlatform,
    val channelId: Long,
    val formatId: Long,
    val postDate: LocalDate,
    val endDate: LocalDate?,
    val status: ChannelAdStatus,
    postUrl: String?,
    creativeMemo: String?,
    adminMemo: String?,
) {
    val postUrl: String? = postUrl?.trim()?.ifEmpty { null }
    val creativeMemo: String? = creativeMemo?.trim()?.ifEmpty { null }
    val adminMemo: String? = adminMemo?.trim()?.ifEmpty { null }

    init {
        require(advertiserId > 0 && channelId > 0 && formatId > 0) { "광고주·채널·유형 식별자는 양수여야 합니다." }
        require(contractId == null || contractId > 0) { "계약 식별자는 양수여야 합니다." }
        require(endDate == null || !endDate.isBefore(postDate)) { "노출 끝 날은 게시일과 같거나 뒤여야 합니다." }
        require(this.postUrl == null || (URL.matches(this.postUrl) && this.postUrl.length <= MAX_URL_LENGTH)) { "게시 확인 주소는 http(s) 주소여야 합니다." }
        require((this.creativeMemo?.length ?: 0) <= MAX_MEMO_LENGTH) { "소재 메모는 ${MAX_MEMO_LENGTH}자 이하여야 합니다." }
        require((this.adminMemo?.length ?: 0) <= MAX_MEMO_LENGTH) { "담당자 메모는 ${MAX_MEMO_LENGTH}자 이하여야 합니다." }
    }

    companion object {
        const val MAX_URL_LENGTH = 500
        const val MAX_MEMO_LENGTH = 1000

        val URL = Regex("^https?://\\S+$")
    }
}
