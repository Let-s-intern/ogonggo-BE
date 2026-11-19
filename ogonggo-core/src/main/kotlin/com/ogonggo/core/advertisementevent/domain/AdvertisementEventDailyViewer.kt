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

/** 계약 기간의 고유 사용자와 클릭 직전 직무 분포를 계산하는 일별 사용자 집합이다. 1년 뒤 식별 정보를 지운다. */
@Entity
@Table(
    name = "advertisement_event_daily_viewers",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_advertisement_daily_viewer",
            columnNames = ["event_date", "exposure_id", "contract_key", "event_type", "viewer_key"],
        ),
    ],
    indexes = [
        Index(name = "idx_advertisement_daily_viewer_contract_date", columnList = "contract_id, event_date"),
        Index(name = "idx_advertisement_daily_viewer_type_date", columnList = "event_type, event_date"),
        Index(name = "idx_advertisement_daily_viewer_event_date", columnList = "event_date"),
    ],
)
class AdvertisementEventDailyViewer internal constructor(
    eventDate: LocalDate,
    exposureId: Long,
    productId: Long,
    contractId: Long?,
    category: AdvertisementProductCategory,
    position: AdvertisementProductPosition?,
    targetType: AdvertisementExposureTargetType?,
    eventType: AdvertisementEventType,
    viewerKey: String,
    memberId: Long?,
    anonymousId: String,
    audienceType: AdvertisementEventAudienceType,
    jobRoles: String?,
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

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    val eventType: AdvertisementEventType = eventType

    @Column(name = "viewer_key", nullable = false, length = AdvertisementEvent.MAX_VIEWER_KEY_LENGTH)
    val viewerKey: String = viewerKey

    @Column(name = "member_id")
    val memberId: Long? = memberId

    @Column(name = "anonymous_id", nullable = false, length = AdvertisementEvent.MAX_ANONYMOUS_ID_LENGTH)
    val anonymousId: String = anonymousId

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 20)
    val audienceType: AdvertisementEventAudienceType = audienceType

    /** 클릭 당시 회원 직무 enum 이름을 쉼표로 이어 저장한다. */
    @Column(name = "job_roles", columnDefinition = "TEXT")
    val jobRoles: String? = jobRoles
}
