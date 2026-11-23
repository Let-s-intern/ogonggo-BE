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
 * 광고 상품 리포트에 붙는 인사이트(제목과 본문) 한 개다. 운영자가 상품마다 직접 써서 기업 회원에게 보낸다.
 *
 * 저장하면 상품의 인사이트를 통째로 바꾸므로(이전 것은 소프트 삭제) 순서는 [displayOrder]가 정한다.
 * [publishedAt]이 있어야 기업 회원의 리포트에 보인다. 없으면 운영자만 보는 임시 저장이다.
 */
@Entity
@Table(
    name = "advertisement_report_insights",
    indexes = [Index(name = "idx_advertisement_report_insights_product", columnList = "contract_product_id, deleted_at")],
)
class AdvertisementReportInsight internal constructor(
    contractProductId: Long,
    title: String,
    body: String,
    displayOrder: Int,
    publishedAt: LocalDateTime?,
) : BaseTimeEntity() {

    init {
        require(contractProductId > 0) { "계약 상품 식별자는 양수여야 합니다." }
        require(title.isNotBlank() && title.length <= MAX_TITLE_LENGTH) { "제목은 1자 이상 ${MAX_TITLE_LENGTH}자 이하여야 합니다." }
        require(body.isNotBlank() && body.length <= MAX_BODY_LENGTH) { "본문은 1자 이상 ${MAX_BODY_LENGTH}자 이하여야 합니다." }
        require(displayOrder >= 0) { "순서는 0 이상이어야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 인사이트 식별자 */
        protected set

    @Column(name = "contract_product_id", nullable = false)
    val contractProductId: Long = contractProductId /* 인사이트가 붙은 계약 상품 식별자 */

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    val title: String = title /* 제목 */

    @Column(nullable = false, length = MAX_BODY_LENGTH)
    val body: String = body /* 본문 */

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int = displayOrder /* 보이는 순서. 작은 것이 먼저 */

    @Column(name = "published_at")
    val publishedAt: LocalDateTime? = publishedAt /* 기업 회원에게 보낸 일시. 없으면 임시 저장 */

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 다른 내용으로 바뀌어 지운 일시 */
        protected set

    val published: Boolean
        get() = publishedAt != null

    companion object {
        const val MAX_TITLE_LENGTH = 60
        const val MAX_BODY_LENGTH = 500
        const val MAX_COUNT = 6
    }
}
