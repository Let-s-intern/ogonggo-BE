package com.ogonggo.core.banner.persistence

import com.ogonggo.core.banner.domain.Banner
import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.core.banner.domain.QBanner.banner
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

internal interface BannerJpaRepository : JpaRepository<Banner, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Banner?

    fun findAllByPlacementAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(placement: BannerPlacement): List<Banner>

    fun findAllByAdvertiserIdAndDeletedAtIsNull(advertiserId: Long): List<Banner>

    fun findAllByAdvertiserIdInAndDeletedAtIsNull(advertiserIds: Collection<Long>): List<Banner>

    fun findAllByContractIdInAndDeletedAtIsNull(contractIds: Collection<Long>): List<Banner>

    /** 계약 연결을 끊을 때 쓴다. 지운 배너도 찾는다. */
    fun findAllByContractId(contractId: Long): List<Banner>

    /** 순서를 매기는 쓰기(등록·삭제·순서 바꾸기)가 겹치지 않도록 그 자리의 배너를 잠가 읽는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Banner b where b.placement = :placement and b.deletedAt is null order by b.displayOrder asc, b.id asc")
    fun findAllInPlacementForUpdate(@Param("placement") placement: BannerPlacement): List<Banner>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Banner b where b.id = :bannerId and b.deletedAt is null")
    fun findForUpdate(@Param("bannerId") bannerId: Long): Banner?

    /** 삭제는 멱등해야 하므로 이미 삭제된 배너도 잠가 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Banner b where b.id = :bannerId")
    fun findIncludingDeletedForUpdate(@Param("bannerId") bannerId: Long): Banner?
}

/** 사용자 웹에 지금 나가는 배너를 고른다. */
@Repository
internal class BannerQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /** [placement]에서 [now]에 노출 중인 배너다. 켜져 있고 시작은 포함, 끝은 포함하지 않는다. 순서대로다. */
    fun findLive(placement: BannerPlacement, now: LocalDateTime): List<Banner> =
        queryFactory.selectFrom(banner)
            .where(
                banner.placement.eq(placement),
                banner.deletedAt.isNull,
                banner.enabled.isTrue,
                banner.startAt.isNull.or(banner.startAt.loe(now)),
                banner.endAt.isNull.or(banner.endAt.gt(now)),
            )
            .orderBy(banner.displayOrder.asc(), banner.id.asc())
            .fetch()
}
