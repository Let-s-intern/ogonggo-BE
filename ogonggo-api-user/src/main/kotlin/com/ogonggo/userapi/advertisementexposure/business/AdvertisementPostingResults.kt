package com.ogonggo.userapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.userapi.bootcamp.business.UserBootcampSummary
import com.ogonggo.userapi.job.business.UserJobSummary

/** 광고 공고 자리의 카드 하나다. [targetType]에 맞춰 [job]과 [bootcamp] 중 하나만 있다. */
data class AdvertisementPostingResult(
    val exposureId: Long,
    val position: AdvertisementProductPosition,
    val jobField: JobField?,
    val displayOrder: Int,
    val adLabel: Boolean,
    val targetType: AdvertisementExposureTargetType,
    val job: UserJobSummary?,
    val bootcamp: UserBootcampSummary?,
)
