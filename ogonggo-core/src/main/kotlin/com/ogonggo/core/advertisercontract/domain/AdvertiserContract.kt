package com.ogonggo.core.advertisercontract.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 광고주가 언제부터 언제까지 얼마를 내고 어느 자리를 샀는지 적은 광고 계약이다. 광고주 하나에 여러 건이 있다.
 *
 * 기간은 시작 포함·끝 미포함이고, 지금이 기간 안이면 진행 중이다. 결제는 운영자가 체크하며 결제 전이면 결제일을 두지 않는다.
 * 배너·광고 공고는 계약 하나에 이어 둘 수 있고, 계약을 지워도 광고는 남기고 잇는 값만 끊는다.
 */
@Entity
@Table(
    name = "advertiser_contracts",
    indexes = [
        Index(name = "idx_advertiser_contracts_advertiser_id", columnList = "advertiser_id, deleted_at"),
    ],
)
class AdvertiserContract internal constructor(
    advertiserId: Long,
    terms: AdvertiserContractTerms,
) : BaseTimeEntity() {

    init {
        require(advertiserId > 0) { "광고주 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 광고 계약 식별자 */
        protected set

    @Column(name = "advertiser_id", nullable = false)
    val advertiserId: Long = advertiserId /* 광고주 식별자 */

    @Column(length = AdvertiserContractTerms.MAX_NAME_LENGTH)
    var name: String? = terms.name /* 계약명 */
        protected set

    @Column(name = "start_at", nullable = false)
    var startAt: LocalDateTime = terms.startAt /* 광고 시작(포함) */
        protected set

    @Column(name = "end_at", nullable = false)
    var endAt: LocalDateTime = terms.endAt /* 광고 끝(미포함) */
        protected set

    @Column(nullable = false)
    var amount: Long = terms.amount /* 금액(원) */
        protected set

    @Column(name = "vat_included", nullable = false)
    var vatIncluded: Boolean = terms.vatIncluded /* 금액이 부가가치세 포함인가 */
        protected set

    @Column(nullable = false)
    var paid: Boolean = terms.paid /* 결제 완료 여부 */
        protected set

    @Column(name = "paid_date")
    var paidDate: LocalDate? = terms.paidDate /* 결제일 */
        protected set

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "advertiser_contract_products", joinColumns = [JoinColumn(name = "contract_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "product", nullable = false, length = 40)
    var products: MutableList<AdContractProduct> = terms.products.toMutableList()
        get() = field.sortedBy { it.ordinal }.toMutableList()
        protected set

    @Column(length = AdvertiserContractTerms.MAX_MEMO_LENGTH)
    var memo: String? = terms.memo /* 운영자 메모 */
        protected set

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "advertiser_contract_channel_formats", joinColumns = [JoinColumn(name = "contract_id")])
    @Column(name = "format_id", nullable = false)
    var channelFormatIds: MutableList<Long> = terms.channelFormatIds.toMutableList() /* 산 채널 게재 유형. 순서는 응답할 때 유형 순서로 맞춘다 */
        get() = field.toMutableList()
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    val terms: AdvertiserContractTerms
        get() = AdvertiserContractTerms(
            name = name,
            startAt = startAt,
            endAt = endAt,
            amount = amount,
            vatIncluded = vatIncluded,
            paid = paid,
            paidDate = paidDate,
            products = products,
            memo = memo,
            channelFormatIds = channelFormatIds,
        )

    /** 계약 내용을 통째로 바꾼다. 부분 수정은 저장된 값과 합친 [terms]를 넘긴다. */
    fun change(terms: AdvertiserContractTerms) {
        check(deletedAt == null) { "삭제된 계약은 수정할 수 없습니다." }
        name = terms.name
        startAt = terms.startAt
        endAt = terms.endAt
        amount = terms.amount
        vatIncluded = terms.vatIncluded
        paid = terms.paid
        paidDate = terms.paidDate
        if (products != terms.products) {
            products = terms.products.toMutableList()
        }
        memo = terms.memo
        if (channelFormatIds.toSet() != terms.channelFormatIds.toSet()) {
            channelFormatIds = terms.channelFormatIds.toMutableList()
        }
    }

    /** 지운 채널 게재 유형을 산 유형에서 뺀다. 없으면 그대로 둔다. */
    fun removeChannelFormat(formatId: Long) {
        if (formatId in channelFormatIds) {
            channelFormatIds = channelFormatIds.filter { it != formatId }.toMutableList()
        }
    }

    /** 지금이 계약 기간 안인가. 시작은 포함하고 끝은 포함하지 않는다. */
    fun isActive(now: LocalDateTime): Boolean = !startAt.isAfter(now) && now.isBefore(endAt)

    /** 삭제는 반복해도 최초 삭제 일시를 유지한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}

/**
 * 계약 내용이다. 이름·메모는 앞뒤 공백을 빼고 비면 null로, 결제 전이면 결제일을 null로, 산 자리는 겹치지 않게 선언 순서로 맞춘다.
 * 산 채널 게재 유형(`channelFormatIds`)은 겹치지 않게 둔다. 유형은 운영자가 더하고 지워 고정된 자리 값인 [products]와 따로 둔다.
 */
class AdvertiserContractTerms(
    name: String?,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val amount: Long,
    val vatIncluded: Boolean,
    val paid: Boolean,
    paidDate: LocalDate?,
    products: Collection<AdContractProduct>,
    memo: String?,
    channelFormatIds: Collection<Long> = emptyList(),
) {
    val name: String? = name?.trim()?.ifEmpty { null }
    val paidDate: LocalDate? = paidDate.takeIf { paid }
    val products: List<AdContractProduct> = products.distinct().sortedBy { it.ordinal }
    val memo: String? = memo?.trim()?.ifEmpty { null }
    val channelFormatIds: List<Long> = channelFormatIds.distinct()

    init {
        require((this.name?.length ?: 0) <= MAX_NAME_LENGTH) { "계약명은 ${MAX_NAME_LENGTH}자 이하여야 합니다." }
        require(startAt.isBefore(endAt)) { "광고 끝은 시작보다 뒤여야 합니다." }
        require(amount in 0..MAX_AMOUNT) { "금액은 0 이상 ${MAX_AMOUNT}원 이하여야 합니다." }
        require((this.memo?.length ?: 0) <= MAX_MEMO_LENGTH) { "메모는 ${MAX_MEMO_LENGTH}자 이하여야 합니다." }
        require(this.channelFormatIds.all { it > 0 }) { "채널 게재 유형 식별자는 양수여야 합니다." }
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_MEMO_LENGTH = 500
        const val MAX_AMOUNT = 9_999_999_999L
    }
}
