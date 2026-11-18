package com.ogonggo.core.advertisementexposure.implement.dto

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import java.time.LocalDateTime

data class AdvertisementExposureEventTargetDto(
    val exposureId: Long,
    val productId: Long,
    val contractId: Long?,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val targetType: AdvertisementExposureTargetType?,
    val targetId: Long?,
    val exposed: Boolean,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
) {
    fun isShownAt(now: LocalDateTime): Boolean =
        exposed && (startAt?.let { !now.isBefore(it) } ?: true) && (endAt?.let { !now.isAfter(it) } ?: true)
}
