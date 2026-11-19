package com.ogonggo.core.advertisementcontract.persistence

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractProduct
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSearchCondition
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractSource
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractTarget
import com.ogonggo.core.advertisementcontract.domain.QAdvertisementContract.advertisementContract
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.QAdvertisementExposure.advertisementExposure
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
import java.time.LocalDate

internal interface AdvertisementContractJpaRepository : JpaRepository<AdvertisementContract, Long> {
    fun findBySourceAndPromotionTargetAndTargetId(
        source: AdvertisementContractSource,
        promotionTarget: AdvertisementContractPromotionTarget,
        targetId: Long,
    ): AdvertisementContract?

    fun findAllByIdIn(ids: Collection<Long>): List<AdvertisementContract>

    fun findAllBySourceAndEmailAndUserIdIsNull(source: AdvertisementContractSource, email: String): List<AdvertisementContract>

    fun findByIdAndUserId(id: Long, userId: Long): AdvertisementContract?

    fun findAllByUserId(userId: Long): List<AdvertisementContract>

    /** 상태와 결제 금액을 동시에 바꿔도 한쪽 변경이 다른 쪽의 낡은 값으로 덮이지 않도록 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AdvertisementContract c where c.id = :id")
    fun findForUpdate(id: Long): AdvertisementContract?
}

internal interface AdvertisementContractProductJpaRepository : JpaRepository<AdvertisementContractProduct, Long> {
    fun findAllByContractIdAndDeletedAtIsNullOrderByIdAsc(contractId: Long): List<AdvertisementContractProduct>

    fun findByIdAndContractIdAndDeletedAtIsNull(id: Long, contractId: Long): AdvertisementContractProduct?
}

internal interface AdvertisementContractTargetJpaRepository : JpaRepository<AdvertisementContractTarget, Long> {
    fun findAllByContractIdAndDeletedAtIsNullOrderByIdAsc(contractId: Long): List<AdvertisementContractTarget>

    fun findByContractIdAndTargetTypeAndTargetId(
        contractId: Long,
        targetType: AdvertisementContractPromotionTarget,
        targetId: Long,
    ): AdvertisementContractTarget?
}

/** 관리자 콘솔 광고 계약 목록과 회원의 광고 신청 내역이다. 생긴 순서와 식별자 순서가 같으므로 최신순은 `id DESC`다. */
@Repository
internal class AdvertisementContractQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun findPage(condition: AdvertisementContractSearchCondition, pageable: Pageable): Page<AdvertisementContract> {
        val predicates = arrayOf(
            condition.source?.let(advertisementContract.source::eq),
            condition.userId?.let(advertisementContract.userId::eq),
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

/**
 * 계약 상품의 성과 리포트가 셀 광고 노출이다. 노출 쪽 Reader가 아니라 여기 두는 것은 리포트가 계약 상품 기준의 조회이기 때문이다.
 * 기간이 지난 뒤 지운 노출도 그 기간의 이벤트를 남겼으므로 함께 준다.
 */
@Repository
internal class AdvertisementContractExposureQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /** 계약·상품이 같고 노출 기간이 [from]~[to](포함)와 겹치는 노출을 자리 안 순서로 준다. 기간 없는 노출은 늘 겹친다. */
    fun findOverlapping(contractId: Long, productId: Long, from: LocalDate, to: LocalDate): List<AdvertisementExposure> {
        val start = from.atStartOfDay()
        val endExclusive = to.plusDays(1).atStartOfDay()
        return queryFactory.selectFrom(advertisementExposure)
            .where(
                advertisementExposure.contractId.eq(contractId),
                advertisementExposure.productId.eq(productId),
                advertisementExposure.startAt.isNull.or(advertisementExposure.startAt.lt(endExclusive)),
                advertisementExposure.endAt.isNull.or(advertisementExposure.endAt.goe(start)),
                advertisementExposure.deletedAt.isNull.or(advertisementExposure.deletedAt.goe(start)),
            )
            .orderBy(advertisementExposure.displayOrder.asc(), advertisementExposure.id.asc())
            .fetch()
    }
}
