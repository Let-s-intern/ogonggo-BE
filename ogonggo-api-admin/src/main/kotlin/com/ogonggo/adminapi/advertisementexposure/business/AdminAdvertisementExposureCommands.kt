package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.job.domain.JobField
import java.time.LocalDateTime

data class CreateAdminAdvertisementExposureCommand(
    val productId: Long,
    val contractId: Long?,
    val name: String?,
    val imageUrl: String?,
    val mobileImageUrl: String?,
    val linkUrl: String?,
    val targetType: AdvertisementExposureTargetType?,
    val targetId: Long?,
    val jobField: JobField?,
    val recommendationTitle: String?,
    val recommendationDescription: String?,
    val adLabel: Boolean,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val exposed: Boolean,
)
