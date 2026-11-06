package com.ogonggo.core.advertisercontract.persistence

import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface AdvertiserContractJpaRepository : JpaRepository<AdvertiserContract, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AdvertiserContract?

    fun findAllByAdvertiserIdAndDeletedAtIsNullOrderByStartAtDescIdDesc(advertiserId: Long): List<AdvertiserContract>

    fun findAllByAdvertiserIdInAndDeletedAtIsNull(advertiserIds: Collection<Long>): List<AdvertiserContract>

    fun findAllByIdInAndDeletedAtIsNull(ids: Collection<Long>): List<AdvertiserContract>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AdvertiserContract c where c.id = :contractId and c.deletedAt is null")
    fun findForUpdate(@Param("contractId") contractId: Long): AdvertiserContract?

    /** 삭제는 멱등해야 하므로 이미 삭제된 계약도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AdvertiserContract c where c.id = :contractId")
    fun findIncludingDeletedForUpdate(@Param("contractId") contractId: Long): AdvertiserContract?
}
