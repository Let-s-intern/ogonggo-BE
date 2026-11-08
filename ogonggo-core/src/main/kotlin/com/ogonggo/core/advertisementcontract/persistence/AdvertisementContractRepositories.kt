package com.ogonggo.core.advertisementcontract.persistence

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractProduct
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSearchCondition
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.QAdvertisementContract.advertisementContract
import com.ogonggo.core.jpa.pageOf
import com.ogonggo.core.jpa.paged
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

internal interface AdvertisementContractJpaRepository : JpaRepository<AdvertisementContract, Long> {
    fun findBySourceAndPromotionTargetAndTargetId(
        source: AdvertisementContractSource,
        promotionTarget: AdvertisementContractPromotionTarget,
        targetId: Long,
    ): AdvertisementContract?

    fun findAllByIdIn(ids: Collection<Long>): List<AdvertisementContract>

    /** 상태와 결제 금액을 동시에 바꿔도 한쪽 변경이 다른 쪽의 낡은 값으로 덮이지 않도록 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AdvertisementContract c where c.id = :id")
    fun findForUpdate(id: Long): AdvertisementContract?
}

internal interface AdvertisementContractProductJpaRepository : JpaRepository<AdvertisementContractProduct, Long> {
    fun findAllByContractIdAndDeletedAtIsNullOrderByIdAsc(contractId: Long): List<AdvertisementContractProduct>

    fun findByIdAndContractIdAndDeletedAtIsNull(id: Long, contractId: Long): AdvertisementContractProduct?
}

/** 관리자 콘솔 광고 계약 목록이다. 생긴 순서와 식별자 순서가 같으므로 최신순은 `id DESC`다. */
@Repository
internal class AdvertisementContractQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPage(condition: AdvertisementContractSearchCondition, pageable: Pageable): Page<AdvertisementContract> {
        val predicates = arrayOf(
            condition.source?.let(advertisementContract.source::eq),
            condition.statuses.takeIf { it.isNotEmpty() }?.let { advertisementContract.status.`in`(it) },
            condition.promotionTarget?.let(advertisementContract.promotionTarget::eq),
            condition.category?.let { advertisementContract.categories.contains(it) },
            condition.companyName?.trim()?.takeIf { it.isNotEmpty() }?.let { advertisementContract.companyName.containsIgnoreCase(it) },
            condition.createdFrom?.let { advertisementContract.createdAt.goe(it.atStartOfDay()) },
            condition.createdTo?.let { advertisementContract.createdAt.lt(it.plusDays(1).atStartOfDay()) },
        )

        val content = queryFactory.selectFrom(advertisementContract)
            .where(*predicates)
            .orderBy(advertisementContract.id.desc())
            .paged(pageable)
            .fetch()

        val countQuery = queryFactory.select(advertisementContract.count())
            .from(advertisementContract)
            .where(*predicates)

        return pageOf(content, pageable, countQuery)
    }

    /** 페이지네이션이 끝난 계약 ID만 대상으로 응답 변환에 필요한 categories를 한 번에 초기화한다. */
    fun initializeCategories(contractIds: Collection<Long>) {
        if (contractIds.isEmpty()) return

        queryFactory.selectFrom(advertisementContract)
            .distinct()
            .leftJoin(advertisementContract.categories).fetchJoin()
            .where(advertisementContract.id.`in`(contractIds))
            .fetch()
    }
}
