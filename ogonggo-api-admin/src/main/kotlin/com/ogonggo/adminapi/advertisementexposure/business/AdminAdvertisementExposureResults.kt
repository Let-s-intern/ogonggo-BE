package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposurePageDto
import java.time.LocalDateTime

data class AdminAdvertisementExposureResult(
    val id: Long,
    val productId: Long,
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
            mobileImageUrl = exposure.mobileImageUrl,
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

data class AdminAdvertisementExposurePageResult(
    val items: List<AdminAdvertisementExposureResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(page: AdvertisementExposurePageDto, companyNames: Map<Long, String>) = AdminAdvertisementExposurePageResult(
            items = page.items.map { exposure ->
                AdminAdvertisementExposureResult.from(
                    exposure,
                    exposure.contractId?.let(companyNames::get),
                )
            },
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
