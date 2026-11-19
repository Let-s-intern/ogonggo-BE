package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 운영자가 계약 상세에서 계약에 붙인 광고 상품이다. 무엇을 팔았는지의 기록이다.
 *
 * 게시 기간([startDate]~[endDate])은 계약한 기간이자 데이터 집계 기간이다. 리포트는 이 기간의 데이터만 보여 준다.
 * 하루 단위이고 종료일도 포함한다. 이 칼럼을 넣기 전에 붙인 상품은 기간이 없다.
 * 노출 기간(광고 노출의 startAt~endAt)과는 다르다. 노출 기간은 실제로 광고가 화면에 노출되는 기간이다.
 *
 * 실제로 언제 어떻게 노출하는지는 노출 설정([com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure])이 가진다.
 * 인스타그램·카카오톡처럼 오공고 화면에 노출하지 않는 상품은 이 기록만 남는다.
 * 금액은 계약 하나에 하나만 두므로 여기에 두지 않는다.
 */
@Entity
@Table(
    name = "advertisement_contract_products",
    indexes = [
        Index(name = "idx_advertisement_contract_products_contract", columnList = "contract_id"),
    ],
)
class AdvertisementContractProduct internal constructor(
    contractId: Long,
    productId: Long,
    period: AdvertisementContractProductPeriod,
) : BaseTimeEntity() {

    init {
        require(contractId > 0 && productId > 0) { "계약과 상품 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 계약 상품 식별자 */
        protected set

    @Column(name = "contract_id", nullable = false)
    val contractId: Long = contractId /* 광고 계약 식별자 */

    @Column(name = "product_id", nullable = false)
    val productId: Long = productId /* 광고 상품 식별자 */

    /** 게시 시작일. 게시 기간은 데이터 집계 기간이고 리포트는 이 기간의 데이터만 보여 준다. 기간을 넣기 전에 붙인 상품은 없다. */
    @Column(name = "start_date")
    var startDate: LocalDate? = period.startDate
        protected set

    /** 게시 종료일(포함). 게시 기간은 데이터 집계 기간이고 리포트는 이 기간의 데이터만 보여 준다. 기간을 넣기 전에 붙인 상품은 없다. */
    @Column(name = "end_date")
    var endDate: LocalDate? = period.endDate
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 계약에서 뺀 일시 */
        protected set

    /**
     * 게시 종료일만 바꾼다(계약 연장·단축). 시작일과 노출 기간은 그대로 둔다.
     * 기간이 없는 상품이거나 새 종료일이 시작일보다 앞이면 [AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_PERIOD_INVALID]다.
     */
    fun changeEndDate(endDate: LocalDate) {
        val startDate = startDate
            ?: throw InvalidValueException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_PERIOD_INVALID)
        this.endDate = AdvertisementContractProductPeriod(startDate, endDate).endDate
    }

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}

/** 계약 상품의 게시 기간이다. 하루 단위이고 시작일과 종료일이 같아도 된다(하루 게시). */
data class AdvertisementContractProductPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    init {
        if (startDate.isAfter(endDate)) {
            throw InvalidValueException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_PERIOD_INVALID)
        }
    }
}
