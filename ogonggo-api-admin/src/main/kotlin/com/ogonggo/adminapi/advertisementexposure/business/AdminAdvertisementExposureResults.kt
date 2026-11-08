package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import java.time.LocalDateTime

data class AdminAdvertisementExposureResult(
    val id: Long,
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
    val advertiserCompanyName: String?,
    val displayOrder: Int,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(exposure: AdvertisementExposure, advertiserCompanyName: String?) = AdminAdvertisementExposureResult(
            id = checkNotNull(exposure.id) { "광고 노출 식별자가 없습니다." },
            productId = exposure.productId,
            contractId = exposure.contractId,
            name = exposure.name,
            imageUrl = exposure.imageUrl,
            linkUrl = exposure.linkUrl,
            targetType = exposure.targetType,
            targetId = exposure.targetId,
            adLabel = exposure.adLabel,
            startAt = exposure.startAt,
            endAt = exposure.endAt,
            exposed = exposure.exposed,
            advertiserCompanyName = advertiserCompanyName,
            displayOrder = exposure.displayOrder,
            registeredAt = exposure.createdAt,
            updatedAt = exposure.updatedAt,
        )
    }
}

data class CreateAdminAdvertisementExposureResult(val id: Long)
