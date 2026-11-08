package com.ogonggo.userapi.advertisementproduct.business

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition

data class AdvertisementProductResult(
    val id: Long,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val name: String,
    val price: Int?,
) {
    companion object {
        fun from(product: AdvertisementProduct): AdvertisementProductResult = AdvertisementProductResult(
            id = checkNotNull(product.id),
            category = product.category,
            position = product.position,
            name = product.name,
            price = product.price,
        )
    }
}
