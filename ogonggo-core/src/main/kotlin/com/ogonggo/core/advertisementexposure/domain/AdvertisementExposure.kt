package com.ogonggo.core.advertisementexposure.domain

import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.error.InvalidValueException
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
 * 광고 상품 자리에 무엇을 띄울지의 설정이다. 운영자가 세팅 화면에서 상품(자리)별로 순서를 정하고 켜고 끈다.
 *
 * 모든 노출 상품이 이 테이블 하나를 쓴다. 상품이 늘어도 행만 늘고, 다른 값이 필요한 노출 종류가 생길 때만 칼럼을 더한다.
 * - 배너: 이름, 이미지, 링크가 있다.
 * - 광고 공고: 띄울 채용공고·부트캠프가 있다.
 *
 * 광고주 계약이 없는 우리 홍보도 둔다([contractId]가 없음). 기간이 없으면 상시다.
 * 노출 중·꺼짐 같은 상태는 켜짐 여부와 기간으로 계산하고 저장하지 않는다.
 */
@Entity
@Table(
    name = "advertisement_exposures",
    indexes = [
        Index(name = "idx_advertisement_exposures_product_order", columnList = "product_id, display_order"),
    ],
)
class AdvertisementExposure internal constructor(
    product: AdvertisementProduct,
    contractId: Long?,
    content: AdvertisementExposureContent,
    adLabel: Boolean,
    period: AdvertisementExposurePeriod,
    exposed: Boolean,
    displayOrder: Int,
) : BaseTimeEntity() {

    init {
        product.checkNotDeleted()
        require(contractId == null || contractId > 0) { "계약 식별자는 양수여야 합니다." }
        validateContent(product.category, content)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 광고 노출 식별자 */
        protected set

    @Column(name = "product_id", nullable = false)
    val productId: Long = checkNotNull(product.id) { "광고 상품 식별자가 없습니다." } /* 노출 자리인 광고 상품 */

    @Column(name = "contract_id")
    var contractId: Long? = contractId /* 광고주 계약. 우리 홍보면 없다 */
        protected set

    @Column(length = MAX_NAME_LENGTH)
    var name: String? = content.name /* 배너 이름 */
        protected set

    @Column(name = "image_url", length = MAX_URL_LENGTH)
    var imageUrl: String? = content.imageUrl /* 배너 이미지 */
        protected set

    @Column(name = "link_url", length = MAX_URL_LENGTH)
    var linkUrl: String? = content.linkUrl /* 배너를 누르면 가는 곳 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    var targetType: AdvertisementExposureTargetType? = content.targetType /* 광고 공고가 띄우는 콘텐츠 종류 */
        protected set

    @Column(name = "target_id")
    var targetId: Long? = content.targetId /* 광고 공고가 띄우는 채용공고·부트캠프 식별자 */
        protected set

    @Column(name = "ad_label", nullable = false)
    var adLabel: Boolean = adLabel /* "광고" 표시 여부 */
        protected set

    @Column(name = "start_at")
    var startAt: LocalDateTime? = period.startAt /* 노출 시작 일시. 없으면 상시 */
        protected set

    @Column(name = "end_at")
    var endAt: LocalDateTime? = period.endAt /* 노출 종료 일시. 없으면 상시 */
        protected set

    @Column(nullable = false)
    var exposed: Boolean = exposed /* 켜짐 여부 */
        protected set

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder /* 같은 자리 안의 순서. 작을수록 먼저 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    /** 노출 자리는 바꾸지 않는다. 다른 자리에 띄우려면 새로 만든다. */
    fun update(
        product: AdvertisementProduct,
        contractId: Long?,
        content: AdvertisementExposureContent,
        adLabel: Boolean,
        period: AdvertisementExposurePeriod,
    ) {
        checkNotDeleted()
        require(product.id == productId) { "노출 자리의 상품이 아닙니다." }
        require(contractId == null || contractId > 0) { "계약 식별자는 양수여야 합니다." }
        validateContent(product.category, content)
        this.contractId = contractId
        name = content.name
        imageUrl = content.imageUrl
        linkUrl = content.linkUrl
        targetType = content.targetType
        targetId = content.targetId
        this.adLabel = adLabel
        startAt = period.startAt
        endAt = period.endAt
    }

    fun changeExposed(exposed: Boolean) {
        checkNotDeleted()
        this.exposed = exposed
    }

    fun changeDisplayOrder(displayOrder: Int) {
        checkNotDeleted()
        this.displayOrder = displayOrder
    }

    /** 켜져 있고 기간 안이면 사용자 화면에 보인다. */
    fun isShownAt(now: LocalDateTime): Boolean =
        deletedAt == null && exposed && (startAt?.let { !now.isBefore(it) } ?: true) && (endAt?.let { !now.isAfter(it) } ?: true)

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    private fun checkNotDeleted() {
        check(deletedAt == null) { "삭제된 광고 노출입니다." }
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_URL_LENGTH = 2048

        private fun validateContent(category: AdvertisementProductCategory, content: AdvertisementExposureContent) {
            when (category) {
                AdvertisementProductCategory.BANNER -> {
                    val valid = !content.name.isNullOrBlank() && !content.imageUrl.isNullOrBlank() && !content.linkUrl.isNullOrBlank() &&
                        content.targetType == null && content.targetId == null
                    if (!valid) {
                        throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_BANNER_CONTENT_REQUIRED)
                    }
                    require(content.name!!.length <= MAX_NAME_LENGTH) { "배너 이름은 ${MAX_NAME_LENGTH}자 이하여야 합니다." }
                    require(content.imageUrl!!.length <= MAX_URL_LENGTH && content.linkUrl!!.length <= MAX_URL_LENGTH) {
                        "주소는 ${MAX_URL_LENGTH}자 이하여야 합니다."
                    }
                }

                AdvertisementProductCategory.JOB_POSTING -> {
                    val valid = content.targetType != null && content.targetId != null && content.targetId > 0 &&
                        content.name == null && content.imageUrl == null && content.linkUrl == null
                    if (!valid) {
                        throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_JOB_POSTING_TARGET_REQUIRED)
                    }
                }

                else -> throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE)
            }
        }
    }
}

/** 노출할 내용이다. 배너는 이름·이미지·링크, 광고 공고는 대상 공고만 채운다. */
data class AdvertisementExposureContent(
    val name: String? = null,
    val imageUrl: String? = null,
    val linkUrl: String? = null,
    val targetType: AdvertisementExposureTargetType? = null,
    val targetId: Long? = null,
)

/** 노출 기간이다. 둘 다 없으면 상시이고, 있으면 둘 다 있어야 한다. */
data class AdvertisementExposurePeriod(
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,
) {
    init {
        val valid = (startAt == null && endAt == null) || (startAt != null && endAt != null && startAt.isBefore(endAt))
        if (!valid) {
            throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_PERIOD_INVALID)
        }
    }

    companion object {
        val ALWAYS = AdvertisementExposurePeriod()
    }
}
