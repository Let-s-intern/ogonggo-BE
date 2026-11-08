package com.ogonggo.userapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure

data class AdvertisementExposureResult(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val linkUrl: String,
    val adLabel: Boolean,
) {
    companion object {
        fun from(exposure: AdvertisementExposure): AdvertisementExposureResult = AdvertisementExposureResult(
            id = checkNotNull(exposure.id),
            name = checkNotNull(exposure.name),
            imageUrl = checkNotNull(exposure.imageUrl),
            linkUrl = checkNotNull(exposure.linkUrl),
            adLabel = exposure.adLabel,
        )
    }
}
