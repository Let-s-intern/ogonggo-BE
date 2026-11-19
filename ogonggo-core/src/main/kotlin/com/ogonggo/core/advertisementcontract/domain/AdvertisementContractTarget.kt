package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 광고 성과 리포트가 "대상 공고 전체 숫자"를 셀 채용공고·부트캠프를 운영자가 계약에 연결한 것이다.
 * 광고 신청 폼 계약은 공고를 가리키지 않아 이 연결로 대상을 정한다. 한 계약에 여러 개를 연결할 수 있다.
 *
 * 비즈니스 공고 계약의 공고([AdvertisementContract.targetId])는 따로 연결하지 않아도 대상이며 이 테이블에 두지 않는다.
 * 연결을 풀면 소프트 삭제하고, 같은 대상을 다시 연결하면 그 행을 되살린다.
 */
@Entity
@Table(
    name = "advertisement_contract_targets",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_advertisement_contract_targets_contract_target",
            columnNames = ["contract_id", "target_type", "target_id"],
        ),
    ],
)
class AdvertisementContractTarget internal constructor(
    contractId: Long,
    targetType: AdvertisementContractPromotionTarget,
    targetId: Long,
) : BaseTimeEntity() {

    init {
        require(contractId > 0 && targetId > 0) { "계약과 대상 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 계약 대상 연결 식별자 */
        protected set

    @Column(name = "contract_id", nullable = false)
    val contractId: Long = contractId /* 광고 계약 식별자 */

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    val targetType: AdvertisementContractPromotionTarget = targetType /* 대상 콘텐츠 종류 */

    @Column(name = "target_id", nullable = false)
    val targetId: Long = targetId /* 대상 채용공고·부트캠프 식별자 */

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 연결을 푼 일시 */
        protected set

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    fun restore() {
        deletedAt = null
    }
}
