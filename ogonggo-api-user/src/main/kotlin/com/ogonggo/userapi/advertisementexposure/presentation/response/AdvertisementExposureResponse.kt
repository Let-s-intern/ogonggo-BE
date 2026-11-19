package com.ogonggo.userapi.advertisementexposure.presentation.response

import com.ogonggo.userapi.advertisementexposure.business.AdvertisementExposureResult
import io.swagger.v3.oas.annotations.media.Schema

data class AdvertisementExposureResponse(
    val id: Long,
    val name: String,
    val imageUrl: String,
    @field:Schema(description = "모바일 배너 이미지. null이면 모바일에서도 imageUrl을 씁니다.", nullable = true)
    val mobileImageUrl: String?,
    val linkUrl: String,
    val adLabel: Boolean,
) {
    companion object {
        internal fun from(result: AdvertisementExposureResult): AdvertisementExposureResponse = AdvertisementExposureResponse(
            id = result.id,
            name = result.name,
            imageUrl = result.imageUrl,
            mobileImageUrl = result.mobileImageUrl,
            linkUrl = result.linkUrl,
            adLabel = result.adLabel,
        )
    }
}
