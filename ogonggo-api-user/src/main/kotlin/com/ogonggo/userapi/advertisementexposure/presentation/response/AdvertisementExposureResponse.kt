package com.ogonggo.userapi.advertisementexposure.presentation.response

import com.ogonggo.userapi.advertisementexposure.business.AdvertisementExposureResult
import com.ogonggo.userapi.advertisementexposure.business.AdvertisementPostingExposureResult
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
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

data class AdvertisementPostingExposureResponse(
    val id: Long,
    val category: AdvertisementProductCategory = AdvertisementProductCategory.JOB_POSTING,
    @field:Schema(description = "광고 공고 category에 별도 position enum이 없으므로 null", nullable = true)
    val position: AdvertisementProductPosition? = null,
    val targetType: AdvertisementExposureTargetType,
    val targetId: Long,
    val displayOrder: Int,
    val adLabel: Boolean,
) {
    companion object {
        fun from(result: AdvertisementPostingExposureResult) = AdvertisementPostingExposureResponse(
            id = result.id,
            targetType = result.targetType,
            targetId = result.targetId,
            displayOrder = result.displayOrder,
            adLabel = result.adLabel,
        )
    }
}
