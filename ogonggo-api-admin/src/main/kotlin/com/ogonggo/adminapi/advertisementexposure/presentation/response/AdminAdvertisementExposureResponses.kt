package com.ogonggo.adminapi.advertisementexposure.presentation.response

import com.ogonggo.adminapi.advertisementexposure.business.AdminAdvertisementExposureResult
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminAdvertisementExposureResponse(
    val id: Long,
    val productId: Long,
    @field:Schema(nullable = true)
    val contractId: Long?,
    @field:Schema(nullable = true)
    val name: String?,
    @field:Schema(nullable = true)
    val imageUrl: String?,
    @field:Schema(description = "모바일 배너 이미지. null이면 모바일에서도 imageUrl을 씁니다.", nullable = true)
    val mobileImageUrl: String?,
    @field:Schema(nullable = true)
    val linkUrl: String?,
    @field:Schema(nullable = true)
    val targetType: AdvertisementExposureTargetType?,
    @field:Schema(nullable = true)
    val targetId: Long?,
    val adLabel: Boolean,
    @field:Schema(nullable = true)
    val startAt: LocalDateTime?,
    @field:Schema(nullable = true)
    val endAt: LocalDateTime?,
    val exposed: Boolean,
    @field:Schema(nullable = true)
    val advertiserCompanyName: String?,
    val displayOrder: Int,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(result: AdminAdvertisementExposureResult) = AdminAdvertisementExposureResponse(
            id = result.id,
            productId = result.productId,
            contractId = result.contractId,
            name = result.name,
            imageUrl = result.imageUrl,
            mobileImageUrl = result.mobileImageUrl,
            linkUrl = result.linkUrl,
            targetType = result.targetType,
            targetId = result.targetId,
            adLabel = result.adLabel,
            startAt = result.startAt,
            endAt = result.endAt,
            exposed = result.exposed,
            advertiserCompanyName = result.advertiserCompanyName,
            displayOrder = result.displayOrder,
            registeredAt = result.registeredAt,
            updatedAt = result.updatedAt,
        )
    }
}

data class CreateAdminAdvertisementExposureResponse(val id: Long) {
    companion object {
        fun from(id: Long) = CreateAdminAdvertisementExposureResponse(id)
    }
}
