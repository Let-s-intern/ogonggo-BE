package com.ogonggo.core.advertisementproduct.persistence

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import org.springframework.data.jpa.repository.JpaRepository

internal interface AdvertisementProductJpaRepository : JpaRepository<AdvertisementProduct, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AdvertisementProduct?

    fun findAllByDeletedAtIsNull(): List<AdvertisementProduct>

    fun findAllByIdIn(ids: Collection<Long>): List<AdvertisementProduct>

    fun findAllByPositionAndDeletedAtIsNull(position: AdvertisementProductPosition): List<AdvertisementProduct>
}
