package com.ogonggo.core.channelad.persistence

import com.ogonggo.core.channelad.domain.ChannelAd
import com.ogonggo.core.channelad.domain.ChannelAdSearchCondition
import com.ogonggo.core.channelad.domain.QChannelAd.channelAd
import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

internal interface ChannelAdJpaRepository : JpaRepository<ChannelAd, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): ChannelAd?

    fun existsByChannelIdAndDeletedAtIsNull(channelId: Long): Boolean

    fun existsByFormatIdAndDeletedAtIsNull(formatId: Long): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ChannelAd a where a.id = :adId and a.deletedAt is null")
    fun findForUpdate(@Param("adId") adId: Long): ChannelAd?

    /** 삭제는 멱등해야 하므로 이미 삭제된 광고도 찾는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ChannelAd a where a.id = :adId")
    fun findIncludingDeletedForUpdate(@Param("adId") adId: Long): ChannelAd?

    /** 지운 계약을 가리키는 광고의 계약 연결만 끊는다. 지운 광고도 함께 끊는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChannelAd a set a.contractId = null, a.updatedAt = :now where a.contractId = :contractId")
    fun unlinkContract(@Param("contractId") contractId: Long, @Param("now") now: LocalDateTime): Int
}

/** 관리자 콘솔 채널 광고 목록과 채널·유형·계약별 광고 수다. 지운 광고는 뺀다. */
@Repository
internal class ChannelAdQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /** 게시일, 식별자 순이다. */
    fun findAll(condition: ChannelAdSearchCondition): List<ChannelAd> =
        queryFactory.selectFrom(channelAd)
            .where(
                channelAd.deletedAt.isNull,
                condition.toExclusive?.let(channelAd.postDate::lt),
                condition.from?.let { from -> channelAd.endDate.goe(from).or(channelAd.endDate.isNull.and(channelAd.postDate.goe(from))) },
                condition.advertiserId?.let(channelAd.advertiserId::eq),
                condition.contractId?.let(channelAd.contractId::eq),
                condition.platform?.let(channelAd.platform::eq),
                condition.channelId?.let(channelAd.channelId::eq),
                condition.formatId?.let(channelAd.formatId::eq),
                condition.status?.let(channelAd.status::eq),
            )
            .orderBy(channelAd.postDate.asc(), channelAd.id.asc())
            .fetch()

    fun countByChannels(channelIds: Collection<Long>): Map<Long, Int> = countBy(channelAd.channelId, channelIds)

    fun countByFormats(formatIds: Collection<Long>): Map<Long, Int> = countBy(channelAd.formatId, formatIds)

    fun countByContracts(contractIds: Collection<Long>): Map<Long, Int> = countBy(channelAd.contractId, contractIds)

    private fun countBy(key: NumberPath<Long>, ids: Collection<Long>): Map<Long, Int> {
        if (ids.isEmpty()) {
            return emptyMap()
        }
        val counts = queryFactory.select(key, channelAd.id.count())
            .from(channelAd)
            .where(channelAd.deletedAt.isNull, key.`in`(ids))
            .groupBy(key)
            .fetch()
            .associate { checkNotNull(it.get(key)) to checkNotNull(it.get(channelAd.id.count())).toInt() }
        return ids.associateWith { counts[it] ?: 0 }
    }
}
