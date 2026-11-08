package com.ogonggo.core.advertisementproduct.persistence

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface AdvertisementProductJpaRepository : JpaRepository<AdvertisementProduct, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AdvertisementProduct?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AdvertisementProduct p where p.id = :id and p.deletedAt is null")
    fun findForUpdate(@Param("id") id: Long): AdvertisementProduct?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AdvertisementProduct p where p.id = :id")
    fun findForOrderUpdate(@Param("id") id: Long): AdvertisementProduct?

    fun findAllByDeletedAtIsNull(): List<AdvertisementProduct>

    fun findAllByIdIn(ids: Collection<Long>): List<AdvertisementProduct>

    fun findAllByPositionAndDeletedAtIsNull(position: AdvertisementProductPosition): List<AdvertisementProduct>
}
