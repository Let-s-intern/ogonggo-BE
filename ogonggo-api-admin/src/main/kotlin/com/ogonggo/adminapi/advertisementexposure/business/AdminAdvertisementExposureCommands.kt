package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import java.time.LocalDateTime

data class CreateAdminAdvertisementExposureCommand(
    val productId: Long,
    val contractId: Long?,
    val name: String?,
    val imageUrl: String?,
    val linkUrl: String?,
    val targetType: AdvertisementExposureTargetType?,
    val targetId: Long?,
    val adLabel: Boolean,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val exposed: Boolean,
)
