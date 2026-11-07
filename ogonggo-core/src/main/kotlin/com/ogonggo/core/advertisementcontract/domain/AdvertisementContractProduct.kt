package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 운영자가 계약 상세에서 계약에 붙인 광고 상품이다. 무엇을 팔았는지의 기록이다.
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

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 계약에서 뺀 일시 */
        protected set

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}
