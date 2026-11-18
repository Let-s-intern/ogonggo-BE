package com.ogonggo.core.advertisementevent.domain

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

/** 일별 이벤트 총량과 그날의 고유 사용자를 보관하는 보고용 집계다. 사용자 키는 포함하지 않는다. */
@Entity
@Table(
    name = "advertisement_daily_metrics",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_advertisement_daily_metric_exposure_type",
            columnNames = ["event_date", "exposure_id", "event_type"],
        ),
    ],
    indexes = [Index(name = "idx_advertisement_daily_metric_contract_date", columnList = "contract_id, event_date")],
)
class AdvertisementDailyMetric internal constructor(
    eventDate: LocalDate,
    exposureId: Long,
    productId: Long,
    contractId: Long?,
    category: AdvertisementProductCategory,
    position: AdvertisementProductPosition?,
    eventType: AdvertisementEventType,
    totalCount: Long,
    uniqueViewerCount: Long,
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val category: AdvertisementProductCategory = category

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    val position: AdvertisementProductPosition? = position

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    val eventType: AdvertisementEventType = eventType

    @Column(name = "total_count", nullable = false)
    val totalCount: Long = totalCount

    @Column(name = "unique_viewer_count", nullable = false)
    val uniqueViewerCount: Long = uniqueViewerCount
}
