package com.ogonggo.userapi.advertisementexposure.presentation.response

import com.ogonggo.userapi.advertisementexposure.business.AdvertisementExposureResult

data class AdvertisementExposureResponse(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val linkUrl: String,
    val adLabel: Boolean,
) {
    companion object {
        internal fun from(result: AdvertisementExposureResult): AdvertisementExposureResponse = AdvertisementExposureResponse(
            id = result.id,
            name = result.name,
            imageUrl = result.imageUrl,
            linkUrl = result.linkUrl,
            adLabel = result.adLabel,
        )
    }
}
