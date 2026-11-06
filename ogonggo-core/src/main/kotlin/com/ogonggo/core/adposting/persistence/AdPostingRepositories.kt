package com.ogonggo.core.adposting.persistence

import com.ogonggo.core.adposting.domain.AdPosting
import com.ogonggo.core.adposting.domain.AdPostingPlacement
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface AdPostingJpaRepository : JpaRepository<AdPosting, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AdPosting?

    fun findAllByPlacementAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(placement: AdPostingPlacement): List<AdPosting>

    fun findAllByAdvertiserIdAndDeletedAtIsNull(advertiserId: Long): List<AdPosting>

    fun findAllByAdvertiserIdInAndDeletedAtIsNull(advertiserIds: Collection<Long>): List<AdPosting>

    fun findAllByContractIdInAndDeletedAtIsNull(contractIds: Collection<Long>): List<AdPosting>

    /** 계약을 지울 때 지운 광고 공고의 연결도 함께 끊으려고 삭제된 행까지 찾는다. */
    fun findAllByContractId(contractId: Long): List<AdPosting>

    fun existsByPlacementAndTargetIdAndDeletedAtIsNull(placement: AdPostingPlacement, targetId: Long): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AdPosting a where a.id = :adPostingId and a.deletedAt is null")
    fun findForUpdate(@Param("adPostingId") adPostingId: Long): AdPosting?

    /** 삭제는 멱등해야 하므로 이미 삭제된 광고 공고도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AdPosting a where a.id = :adPostingId")
    fun findIncludingDeletedForUpdate(@Param("adPostingId") adPostingId: Long): AdPosting?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select a from AdPosting a
        where a.placement = :placement and a.deletedAt is null
        order by a.displayOrder asc, a.id asc
        """,
    )
    fun findAllByPlacementForUpdate(@Param("placement") placement: AdPostingPlacement): List<AdPosting>
}
