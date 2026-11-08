package com.ogonggo.adminapi.advertisementproduct.business

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import java.time.LocalDateTime

data class AdminAdvertisementProductResult(
    val id: Long,
    val category: AdvertisementProductCategory,
    val position: AdvertisementProductPosition?,
    val name: String,
    val price: Int?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(product: AdvertisementProduct): AdminAdvertisementProductResult = AdminAdvertisementProductResult(
            id = checkNotNull(product.id),
            category = product.category,
            position = product.position,
            name = product.name,
            price = product.price,
            registeredAt = product.createdAt,
            updatedAt = product.updatedAt,
        )
    }
}
