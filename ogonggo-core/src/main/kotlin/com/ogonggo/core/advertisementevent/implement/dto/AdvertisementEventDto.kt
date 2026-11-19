package com.ogonggo.core.advertisementevent.implement.dto

import com.ogonggo.core.advertisementevent.domain.AdvertisementEventAudienceType
import com.ogonggo.core.advertisementevent.domain.AdvertisementEventType
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.job.domain.JobRole
import java.time.LocalDate
import java.time.LocalDateTime

data class AdvertisementEventAppendDto(
    val eventDate: LocalDate,
    val eventAt: LocalDateTime,
    val exposureId: Long,
    val productId: Long,
    val contractId: Long?,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val targetType: AdvertisementExposureTargetType?,
    val targetId: Long?,
    val eventType: AdvertisementEventType,
    val memberId: Long?,
    val anonymousId: String,
    val viewerKey: String,
    val idempotencyKey: String,
    val audienceType: AdvertisementEventAudienceType,
    val jobRoles: Set<JobRole>,
)
