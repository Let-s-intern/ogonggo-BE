package com.ogonggo.core.advertisercontract.persistence

import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface AdvertiserContractJpaRepository : JpaRepository<AdvertiserContract, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AdvertiserContract?

    fun findAllByAdvertiserIdAndDeletedAtIsNullOrderByStartAtDescIdDesc(advertiserId: Long): List<AdvertiserContract>

    fun findAllByAdvertiserIdInAndDeletedAtIsNull(advertiserIds: Collection<Long>): List<AdvertiserContract>

    fun findAllByIdInAndDeletedAtIsNull(ids: Collection<Long>): List<AdvertiserContract>

    /**
     * 기간이 `[from, toExclusive)`와 겹치는 계약이다. 시작, 식별자 순이다. 서비스가 트랜잭션 없이 읽어도 산 자리를
     * 쓸 수 있도록 함께 읽는다.
     */
    @Query(
        """
        select distinct c from AdvertiserContract c left join fetch c.products
        where c.deletedAt is null and c.startAt < :toExclusive and c.endAt > :from
        order by c.startAt asc, c.id asc
        """,
    )
    fun findAllOverlapping(@Param("from") from: LocalDateTime, @Param("toExclusive") toExclusive: LocalDateTime): List<AdvertiserContract>

    /** 지운 채널 게재 유형을 산 계약이다. */
    @Query("select distinct c from AdvertiserContract c join c.channelFormatIds f where f = :formatId and c.deletedAt is null")
    fun findAllByChannelFormatId(@Param("formatId") formatId: Long): List<AdvertiserContract>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AdvertiserContract c where c.id = :contractId and c.deletedAt is null")
    fun findForUpdate(@Param("contractId") contractId: Long): AdvertiserContract?

    /** 삭제는 멱등해야 하므로 이미 삭제된 계약도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AdvertiserContract c where c.id = :contractId")
    fun findIncludingDeletedForUpdate(@Param("contractId") contractId: Long): AdvertiserContract?
}
