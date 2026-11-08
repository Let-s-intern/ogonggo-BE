package com.ogonggo.adminapi.advertisementproduct.business

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto

data class AdminAdvertisementProductSaveCommand(
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val name: String,
    val price: Int?,
) {
    internal fun toCoreDto(): AdvertisementProductSaveDto = AdvertisementProductSaveDto(
        category = category,
        position = position,
        name = name,
        price = price,
    )
}
