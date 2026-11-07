package com.ogonggo.core.advertisementproduct.implement

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.advertisementproduct.persistence.AdvertisementProductJpaRepository
import org.springframework.stereotype.Component

@Component
class AdvertisementProductAppender internal constructor(
    private val advertisementProductRepository: AdvertisementProductJpaRepository,
) {

    fun append(dto: AdvertisementProductSaveDto): Long {
        val saved = advertisementProductRepository.save(
            AdvertisementProduct(category = dto.category, position = dto.position, name = dto.name, price = dto.price),
        )
        return checkNotNull(saved.id) { "광고 상품 식별자가 없습니다." }
    }
}
