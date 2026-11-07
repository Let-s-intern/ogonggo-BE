package com.ogonggo.core.advertisementexposure.persistence

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.QAdvertisementExposure.advertisementExposure
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

internal interface AdvertisementExposureJpaRepository : JpaRepository<AdvertisementExposure, Long> {
    fun findAllByProductIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(productId: Long): List<AdvertisementExposure>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from AdvertisementExposure e where e.id = :id and e.deletedAt is null")
    fun findForUpdate(id: Long): AdvertisementExposure?
}

@Repository
internal class AdvertisementExposureQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /** 이 상품들 자리에서 지금 보이는 노출이다. 켜져 있고 기간 안([AdvertisementExposure.isShownAt])인 것을 순서대로 준다. */
    fun findShown(productIds: Collection<Long>, now: LocalDateTime): List<AdvertisementExposure> {
        if (productIds.isEmpty()) {
            return emptyList()
        }
        return queryFactory.selectFrom(advertisementExposure)
            .where(
                advertisementExposure.productId.`in`(productIds),
                advertisementExposure.exposed.isTrue,
                advertisementExposure.deletedAt.isNull,
                advertisementExposure.startAt.isNull.or(advertisementExposure.startAt.loe(now)),
                advertisementExposure.endAt.isNull.or(advertisementExposure.endAt.goe(now)),
            )
            .orderBy(advertisementExposure.displayOrder.asc(), advertisementExposure.id.asc())
            .fetch()
    }
}
