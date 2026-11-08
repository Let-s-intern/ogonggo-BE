package com.ogonggo.core.advertisementproduct.persistence

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.domain.QAdvertisementProduct.advertisementProduct
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
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

@org.springframework.stereotype.Repository
internal class AdvertisementProductQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {
    fun findActivePage(pageable: Pageable): Page<AdvertisementProduct> {
        val categoryOrder = CaseBuilder()
            .`when`(advertisementProduct.category.eq(AdvertisementProductCategory.JOB_POSTING)).then(1)
            .`when`(advertisementProduct.category.eq(AdvertisementProductCategory.BANNER)).then(2)
            .`when`(advertisementProduct.category.eq(AdvertisementProductCategory.INSTAGRAM_1)).then(3)
            .`when`(advertisementProduct.category.eq(AdvertisementProductCategory.INSTAGRAM_2)).then(4)
            .`when`(advertisementProduct.category.eq(AdvertisementProductCategory.KAKAO_1)).then(5)
            .`when`(advertisementProduct.category.eq(AdvertisementProductCategory.KAKAO_2)).then(6)
            .otherwise(Int.MAX_VALUE)
        val products = queryFactory.selectFrom(advertisementProduct)
            .where(advertisementProduct.deletedAt.isNull)
            .orderBy(categoryOrder.asc(), advertisementProduct.id.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        val totalElements = queryFactory.select(advertisementProduct.id.count())
            .from(advertisementProduct)
            .where(advertisementProduct.deletedAt.isNull)
            .fetchOne() ?: 0L
        return PageImpl(products, pageable, totalElements)
    }
}
