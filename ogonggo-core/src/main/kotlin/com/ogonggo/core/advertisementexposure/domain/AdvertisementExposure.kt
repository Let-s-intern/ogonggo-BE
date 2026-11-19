package com.ogonggo.core.advertisementexposure.domain

import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.TodayJob
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
 * - 배너: 이름, 이미지, 링크가 있다. 모바일 이미지는 선택이며, 없으면 모바일에서도 이미지를 쓴다.
 * - 광고 공고: 띄울 채용공고·부트캠프가 있다. 채용공고 목록 자리(JOB_LIST)는 어느 직군 탭에 띄울지([jobField])도 있고,
 *   비어 있으면 전체 탭에 띄운다.
 *   오공고 PICK 자리(JOB_PICK)는 운영자가 고른 오늘의 공고([TodayJob])처럼 카드에 보일 추천 문구도 있다.
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
        validateContent(product, content)
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

    @Column(name = "mobile_image_url", length = MAX_URL_LENGTH)
    var mobileImageUrl: String? = content.mobileImageUrl /* 배너 모바일 이미지. 없으면 모바일에서도 image_url을 쓴다 */
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

    @Enumerated(EnumType.STRING)
    @Column(name = "job_field", length = 40)
    var jobField: JobField? = content.jobField /* 채용공고 목록 자리에서 띄울 직군 탭. 없으면 전체 탭이고, 다른 자리는 늘 없다 */
        protected set

    @Column(name = "recommendation_title", length = MAX_RECOMMENDATION_TITLE_LENGTH)
    var recommendationTitle: String? = content.recommendationTitle /* 오공고 PICK 카드에 굵게 보여 주는 추천 문구 제목. 다른 자리는 없다 */
        protected set

    @Column(name = "recommendation_description", length = MAX_RECOMMENDATION_DESCRIPTION_LENGTH)
    var recommendationDescription: String? = content.recommendationDescription /* 추천 문구 제목 아래 설명. 다른 자리는 없다 */
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
        validateContent(product, content)
        this.contractId = contractId
        name = content.name
        imageUrl = content.imageUrl
        mobileImageUrl = content.mobileImageUrl
        linkUrl = content.linkUrl
        targetType = content.targetType
        targetId = content.targetId
        jobField = content.jobField
        recommendationTitle = content.recommendationTitle
        recommendationDescription = content.recommendationDescription
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

        /** 오공고 PICK 탭에서 운영자 PICK 카드와 같은 추천 문구 상자에 들어가므로 길이도 [TodayJob]과 같다. */
        const val MAX_RECOMMENDATION_TITLE_LENGTH = TodayJob.RECOMMENDATION_TITLE_MAX_LENGTH
        const val MAX_RECOMMENDATION_DESCRIPTION_LENGTH = TodayJob.RECOMMENDATION_DESCRIPTION_MAX_LENGTH

        private fun validateContent(product: AdvertisementProduct, content: AdvertisementExposureContent) {
            when (product.category) {
                AdvertisementProductCategory.BANNER -> {
                    val valid = !content.name.isNullOrBlank() && !content.imageUrl.isNullOrBlank() && !content.linkUrl.isNullOrBlank() &&
                        (content.mobileImageUrl == null || content.mobileImageUrl.isNotBlank()) &&
                        content.targetType == null && content.targetId == null && content.jobField == null &&
                        content.recommendationTitle == null && content.recommendationDescription == null
                    if (!valid) {
                        throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_BANNER_CONTENT_REQUIRED)
                    }
                    require(content.name!!.length <= MAX_NAME_LENGTH) { "배너 이름은 ${MAX_NAME_LENGTH}자 이하여야 합니다." }
                    require(
                        content.imageUrl!!.length <= MAX_URL_LENGTH && content.linkUrl!!.length <= MAX_URL_LENGTH &&
                            (content.mobileImageUrl?.length ?: 0) <= MAX_URL_LENGTH,
                    ) {
                        "주소는 ${MAX_URL_LENGTH}자 이하여야 합니다."
                    }
                }

                AdvertisementProductCategory.JOB_POSTING -> {
                    val valid = content.targetType != null && content.targetId != null && content.targetId > 0 &&
                        content.name == null && content.imageUrl == null && content.mobileImageUrl == null && content.linkUrl == null
                    if (!valid) {
                        throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_JOB_POSTING_TARGET_REQUIRED)
                    }
                    val expectedTargetType = product.position?.let(::targetTypeOf)
                    if (expectedTargetType != null && expectedTargetType != content.targetType) {
                        throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_TARGET_TYPE_MISMATCH)
                    }
                    // 채용공고 목록 광고는 직군 탭(직군이 없으면 전체 탭)마다 따로 띄운다. 직군 탭이 없는 자리에 직군을 두면 어디에도 안 보인다.
                    if (product.position != AdvertisementProductPosition.JOB_LIST && content.jobField != null) {
                        throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_JOB_FIELD_INVALID)
                    }
                    // 오공고 PICK 광고 카드는 운영자 PICK 카드와 같은 추천 문구 상자를 보인다. 다른 자리에는 문구를 보일 곳이 없다.
                    val recommendationValid = if (product.position == AdvertisementProductPosition.JOB_PICK) {
                        isRecommendationText(content.recommendationTitle, MAX_RECOMMENDATION_TITLE_LENGTH) &&
                            isRecommendationText(content.recommendationDescription, MAX_RECOMMENDATION_DESCRIPTION_LENGTH)
                    } else {
                        content.recommendationTitle == null && content.recommendationDescription == null
                    }
                    if (!recommendationValid) {
                        throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_RECOMMENDATION_INVALID)
                    }
                }

                else -> throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE)
            }
        }

        private fun isRecommendationText(text: String?, maxLength: Int): Boolean =
            !text.isNullOrBlank() && text.length <= maxLength

        /** 광고 공고 자리마다 띄울 수 있는 콘텐츠 종류다. 부트캠프 목록은 부트캠프, 나머지는 채용공고다. */
        private fun targetTypeOf(position: AdvertisementProductPosition): AdvertisementExposureTargetType? = when (position) {
            AdvertisementProductPosition.JOB_LIST,
            AdvertisementProductPosition.JOB_DETAIL_SIDE,
            AdvertisementProductPosition.JOB_PICK,
            AdvertisementProductPosition.CALENDAR_DAY,
            AdvertisementProductPosition.MYPAGE_SCRAP,
            -> AdvertisementExposureTargetType.JOB
            AdvertisementProductPosition.BOOTCAMP_LIST -> AdvertisementExposureTargetType.BOOTCAMP
            else -> null
        }
    }
}

/**
 * 노출할 내용이다. 배너는 이름·이미지·링크(모바일 이미지는 선택), 광고 공고는 대상 공고만 채운다.
 * 광고 공고 중 채용공고 목록 자리는 직군(비우면 전체 탭)을, 오공고 PICK 자리는 추천 문구 제목·설명을 더 채운다.
 */
data class AdvertisementExposureContent(
    val name: String? = null,
    val imageUrl: String? = null,
    val linkUrl: String? = null,
    val targetType: AdvertisementExposureTargetType? = null,
    val targetId: Long? = null,
    val mobileImageUrl: String? = null,
    val jobField: JobField? = null,
    val recommendationTitle: String? = null,
    val recommendationDescription: String? = null,
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
