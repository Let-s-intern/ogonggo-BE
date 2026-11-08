package com.ogonggo.userapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposurePageDto

data class AdvertisementExposurePageResult(
    val items: List<AdvertisementExposureResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(page: AdvertisementExposurePageDto) = AdvertisementExposurePageResult(
            items = page.items.map(AdvertisementExposureResult::from),
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}

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
