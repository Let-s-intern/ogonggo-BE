package com.ogonggo.core.advertisementproduct.implement

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.advertisementproduct.persistence.AdvertisementProductJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdvertisementProductManager internal constructor(
    private val advertisementProductRepository: AdvertisementProductJpaRepository,
) {

    fun update(product: AdvertisementProduct, dto: AdvertisementProductSaveDto) {
        product.update(category = dto.category, position = dto.position, name = dto.name, price = dto.price)
        advertisementProductRepository.save(product)
    }

    /** 이미 맺은 계약과 노출은 그대로 둔다. */
    fun delete(product: AdvertisementProduct, now: LocalDateTime) {
        product.delete(now)
        advertisementProductRepository.save(product)
    }
}
