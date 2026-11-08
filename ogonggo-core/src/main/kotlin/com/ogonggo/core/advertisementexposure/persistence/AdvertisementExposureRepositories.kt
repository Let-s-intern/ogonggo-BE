package com.ogonggo.core.advertisementexposure.persistence

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.QAdvertisementExposure.advertisementExposure
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

internal interface AdvertisementExposureJpaRepository : JpaRepository<AdvertisementExposure, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AdvertisementExposure?

    /** Lock ordering starts with the immutable product key without managing the exposure entity. */
    @Query("select e.productId from AdvertisementExposure e where e.id = :id and e.deletedAt is null")
    fun findProductIdByIdAndDeletedAtIsNull(id: Long): Long?

    /**
     * 순서를 바꾸거나 마지막 순서를 계산하는 트랜잭션용 current read다. MySQL REPEATABLE READ에서도
     * 앞선 일반 조회가 만든 snapshot 대신 잠금 시점의 커밋된 형제 행을 읽는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select e from AdvertisementExposure e " +
            "where e.productId = :productId and e.deletedAt is null " +
            "order by e.displayOrder asc, e.id asc",
    )
    fun findAllForOrderUpdate(productId: Long): List<AdvertisementExposure>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from AdvertisementExposure e where e.id = :id and e.deletedAt is null")
    fun findForUpdate(id: Long): AdvertisementExposure?
}

@Repository
internal class AdvertisementExposureQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /** 상품 자리의 미삭제 노출을 순서대로 준다. 계약 ID는 지정된 경우에만 DB 조건으로 적용한다. */
    fun findByProduct(productId: Long, contractId: Long?): List<AdvertisementExposure> {
        val condition = BooleanBuilder()
            .and(advertisementExposure.productId.eq(productId))
            .and(advertisementExposure.deletedAt.isNull)
        contractId?.let { condition.and(advertisementExposure.contractId.eq(it)) }

        return queryFactory.selectFrom(advertisementExposure)
            .where(condition)
            .orderBy(advertisementExposure.displayOrder.asc(), advertisementExposure.id.asc())
            .fetch()
    }

    fun findByProductPage(productId: Long, contractId: Long?, pageable: Pageable): Page<AdvertisementExposure> {
        val condition = BooleanBuilder()
            .and(advertisementExposure.productId.eq(productId))
            .and(advertisementExposure.deletedAt.isNull)
        contractId?.let { condition.and(advertisementExposure.contractId.eq(it)) }

        val items = queryFactory.selectFrom(advertisementExposure)
            .where(condition)
            .orderBy(advertisementExposure.displayOrder.asc(), advertisementExposure.id.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        val totalElements = queryFactory.select(advertisementExposure.id.count())
            .from(advertisementExposure)
            .where(condition)
            .fetchOne() ?: 0L
        return PageImpl(items, pageable, totalElements)
    }

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

    fun findShownPage(productIds: Collection<Long>, now: LocalDateTime, pageable: Pageable): Page<AdvertisementExposure> {
        if (productIds.isEmpty()) return PageImpl(emptyList(), pageable, 0)
        val condition = BooleanBuilder()
            .and(advertisementExposure.productId.`in`(productIds))
            .and(advertisementExposure.exposed.isTrue)
            .and(advertisementExposure.deletedAt.isNull)
            .and(advertisementExposure.startAt.isNull.or(advertisementExposure.startAt.loe(now)))
            .and(advertisementExposure.endAt.isNull.or(advertisementExposure.endAt.goe(now)))
        val items = queryFactory.selectFrom(advertisementExposure)
            .where(condition)
            .orderBy(advertisementExposure.displayOrder.asc(), advertisementExposure.id.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        val totalElements = queryFactory.select(advertisementExposure.id.count())
            .from(advertisementExposure)
            .where(condition)
            .fetchOne() ?: 0L
        return PageImpl(items, pageable, totalElements)
    }
}
