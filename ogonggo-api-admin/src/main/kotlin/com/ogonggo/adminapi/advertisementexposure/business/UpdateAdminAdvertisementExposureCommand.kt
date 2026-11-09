package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import java.time.LocalDateTime

data class UpdateAdminAdvertisementExposureCommand(
    val exposureId: Long,
    val contractId: Long?,
    val name: String?,
    val imageUrl: String?,
    val mobileImageUrl: String?,
    val linkUrl: String?,
    val targetType: AdvertisementExposureTargetType?,
    val targetId: Long?,
    val adLabel: Boolean,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
)
