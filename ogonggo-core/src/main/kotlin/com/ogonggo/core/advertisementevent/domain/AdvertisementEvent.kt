package com.ogonggo.core.advertisementevent.domain

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
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
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime

/** 사용자 화면에 광고 노출이 실제로 보였거나 클릭된 사실이다. 노출 설정과 당시 상품 정보를 스냅샷으로 남긴다. */
@Entity
@Table(
    name = "advertisement_events",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_advertisement_event_daily_impression",
            columnNames = ["exposure_id", "event_date", "contract_key", "event_type", "deduplication_key"],
        ),
        UniqueConstraint(name = "uk_advertisement_event_idempotency_key", columnNames = ["idempotency_key"]),
    ],
    indexes = [
        Index(name = "idx_advertisement_event_contract_date", columnList = "contract_id, event_date"),
        Index(name = "idx_advertisement_event_exposure_date", columnList = "exposure_id, event_date"),
        Index(name = "idx_advertisement_event_event_date", columnList = "event_date"),
    ],
)
class AdvertisementEvent internal constructor(
    eventDate: LocalDate,
    exposureId: Long,
    productId: Long,
    contractId: Long?,
    category: AdvertisementProductCategory,
    position: AdvertisementProductPosition?,
    targetType: AdvertisementExposureTargetType?,
    targetId: Long?,
    eventType: AdvertisementEventType,
    eventAt: LocalDateTime,
    memberId: Long?,
    anonymousId: String,
    viewerKey: String,
    deduplicationKey: String?,
    idempotencyKey: String,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "event_date", nullable = false)
    val eventDate: LocalDate = eventDate

    @Column(name = "exposure_id", nullable = false)
    val exposureId: Long = exposureId

    @Column(name = "product_id", nullable = false)
    val productId: Long = productId

    @Column(name = "contract_id")
    val contractId: Long? = contractId

    @Column(name = "contract_key", nullable = false)
    val contractKey: Long = contractId ?: 0L

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val category: AdvertisementProductCategory = category

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    val position: AdvertisementProductPosition? = position

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    val targetType: AdvertisementExposureTargetType? = targetType

    @Column(name = "target_id")
    val targetId: Long? = targetId

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    val eventType: AdvertisementEventType = eventType

    @Column(name = "event_at", nullable = false)
    val eventAt: LocalDateTime = eventAt

    @Column(name = "member_id")
    val memberId: Long? = memberId

    @Column(name = "anonymous_id", nullable = false, length = MAX_ANONYMOUS_ID_LENGTH)
    val anonymousId: String = anonymousId

    @Column(name = "viewer_key", nullable = false, length = MAX_VIEWER_KEY_LENGTH)
    val viewerKey: String = viewerKey

    /** 노출만 사용자·노출·날짜별로 한 번 저장한다. 클릭은 null이라 반복 기록할 수 있다. */
    @Column(name = "deduplication_key", length = MAX_VIEWER_KEY_LENGTH)
    val deduplicationKey: String? = deduplicationKey

    @Column(name = "idempotency_key", length = MAX_IDEMPOTENCY_KEY_LENGTH)
    val idempotencyKey: String? = idempotencyKey

    init {
        require(exposureId > 0 && productId > 0) { "광고 노출과 상품 식별자는 양수여야 합니다." }
        require(contractId == null || contractId > 0) { "광고 계약 식별자는 양수여야 합니다." }
        require(memberId == null || memberId > 0) { "회원 식별자는 양수여야 합니다." }
        require(anonymousId.isNotBlank() && anonymousId.length <= MAX_ANONYMOUS_ID_LENGTH) { "익명 식별자는 1~${MAX_ANONYMOUS_ID_LENGTH}자여야 합니다." }
        require(viewerKey.isNotBlank() && viewerKey.length <= MAX_VIEWER_KEY_LENGTH) { "사용자 식별 키가 올바르지 않습니다." }
        require(idempotencyKey.isNotBlank() && idempotencyKey.length <= MAX_IDEMPOTENCY_KEY_LENGTH) { "멱등 키는 1~${MAX_IDEMPOTENCY_KEY_LENGTH}자여야 합니다." }
        require((eventType == AdvertisementEventType.IMPRESSION) == (deduplicationKey != null)) {
            "노출만 중복 제거 키를 가져야 합니다."
        }
    }

    companion object {
        const val MAX_ANONYMOUS_ID_LENGTH = 128
        const val MAX_VIEWER_KEY_LENGTH = 140
        const val MAX_IDEMPOTENCY_KEY_LENGTH = 36
    }
}
