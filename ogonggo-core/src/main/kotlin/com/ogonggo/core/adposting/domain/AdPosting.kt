package com.ogonggo.core.adposting.domain

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
import java.time.LocalDateTime

/**
 * 운영자가 사용자 웹 목록 자리마다 맨 앞 칸부터 싣는 채용공고·부트캠프 광고다. 자리 안 순서(`displayOrder`)가 곧 칸 번호다.
 *
 * 자리와 대상은 등록한 뒤 바꾸지 않는다. 대상의 종류는 자리가 정한다. 같은 자리에 같은 대상은 한 번만 두며, 소프트 삭제한 행과
 * 유일 제약이 부딪히지 않도록 DB 제약 대신 등록할 때 확인한다.
 * 노출 중은 켜져 있고 기간 안(시작 포함·끝 미포함)인 것이다. 대상이 공개·모집 중인지는 대상 쪽 상태라 여기서 보지 않는다.
 * 광고 계약은 같은 광고주의 것만 잇고, 계약을 지우면 잇는 값만 끊는다.
 */
@Entity
@Table(
    name = "ad_postings",
    indexes = [
        Index(name = "idx_ad_postings_placement", columnList = "placement, deleted_at, display_order"),
        Index(name = "idx_ad_postings_advertiser_id", columnList = "advertiser_id, deleted_at"),
        Index(name = "idx_ad_postings_contract_id", columnList = "contract_id"),
    ],
)
class AdPosting internal constructor(
    placement: AdPostingPlacement,
    targetId: Long,
    advertiserId: Long,
    contractId: Long?,
    period: AdPostingPeriod,
    enabled: Boolean,
    displayOrder: Int,
) : BaseTimeEntity() {

    init {
        require(targetId > 0) { "광고 대상 식별자는 양수여야 합니다." }
        validateLinks(advertiserId, contractId)
        require(displayOrder > 0) { "광고 공고 순서는 1부터입니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 광고 공고 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    val placement: AdPostingPlacement = placement /* 자리 */

    @Column(name = "target_id", nullable = false, updatable = false)
    val targetId: Long = targetId /* 채용공고 또는 부트캠프 식별자 */

    @Column(name = "advertiser_id", nullable = false)
    var advertiserId: Long = advertiserId /* 광고주 식별자 */
        protected set

    @Column(name = "contract_id")
    var contractId: Long? = contractId /* 광고 계약 식별자 */
        protected set

    @Column(name = "start_at")
    var startAt: LocalDateTime? = period.startAt /* 노출 시작(포함). 없으면 켜는 즉시 */
        protected set

    @Column(name = "end_at")
    var endAt: LocalDateTime? = period.endAt /* 노출 끝(미포함). 없으면 끌 때까지 */
        protected set

    @Column(nullable = false)
    var enabled: Boolean = enabled /* 켜짐 여부 */
        protected set

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder /* 자리 안 순서이자 칸 번호(1부터) */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    val targetType: AdPostingTargetType
        get() = placement.targetType

    val period: AdPostingPeriod
        get() = AdPostingPeriod(startAt, endAt)

    /** 광고주·계약·기간·켜짐을 통째로 바꾼다. 부분 수정은 저장된 값과 합친 값을 넘긴다. */
    fun change(advertiserId: Long, contractId: Long?, period: AdPostingPeriod, enabled: Boolean) {
        check(deletedAt == null) { "삭제된 광고 공고는 수정할 수 없습니다." }
        validateLinks(advertiserId, contractId)
        this.advertiserId = advertiserId
        this.contractId = contractId
        this.startAt = period.startAt
        this.endAt = period.endAt
        this.enabled = enabled
    }

    fun moveTo(displayOrder: Int) {
        require(displayOrder > 0) { "광고 공고 순서는 1부터입니다." }
        this.displayOrder = displayOrder
    }

    /** 이어 둔 계약을 지웠을 때 잇는 값만 끊는다. */
    fun unlinkContract() {
        contractId = null
    }

    /** 지금 켜져 있고 기간 안인가. 대상의 공개·모집 여부는 보지 않는다. */
    fun isLive(now: LocalDateTime): Boolean = enabled && period.contains(now)

    /** 삭제는 반복해도 최초 삭제 일시를 유지한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    private fun validateLinks(advertiserId: Long, contractId: Long?) {
        require(advertiserId > 0) { "광고주 식별자는 양수여야 합니다." }
        require(contractId == null || contractId > 0) { "광고 계약 식별자는 양수여야 합니다." }
    }
}

/** 노출 기간이다. 시작은 포함하고 끝은 포함하지 않는다. 한쪽이 없으면 그쪽이 열려 있다. 둘 다 있으면 시작이 끝보다 앞이다. */
data class AdPostingPeriod(
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
) {
    init {
        require(startAt == null || endAt == null || startAt.isBefore(endAt)) { "노출 끝은 시작보다 뒤여야 합니다." }
    }

    fun contains(now: LocalDateTime): Boolean =
        (startAt == null || !startAt.isAfter(now)) && (endAt == null || now.isBefore(endAt))

    companion object {
        fun isValid(startAt: LocalDateTime?, endAt: LocalDateTime?): Boolean =
            startAt == null || endAt == null || startAt.isBefore(endAt)
    }
}
