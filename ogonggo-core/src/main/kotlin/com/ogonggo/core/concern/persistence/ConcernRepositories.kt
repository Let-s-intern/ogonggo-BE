package com.ogonggo.core.concern.persistence

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernMetric
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface ConcernJpaRepository : JpaRepository<Concern, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Concern?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select concern from Concern concern where concern.id = :concernId and concern.deletedAt is null")
    fun findActiveByIdForUpdate(@Param("concernId") concernId: Long): Concern?

    /** 삭제를 반복 요청해도 같은 결과를 주려고 삭제된 고민글도 읽는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select concern from Concern concern where concern.id = :concernId")
    fun findByIdForUpdate(@Param("concernId") concernId: Long): Concern?
}

internal interface ConcernMetricJpaRepository : JpaRepository<ConcernMetric, Long> {

    fun findByConcernId(concernId: Long): ConcernMetric?

    fun findAllByConcernIdIn(concernIds: Collection<Long>): List<ConcernMetric>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ConcernMetric metric
        set metric.viewCount = metric.viewCount + 1,
            metric.updatedAt = :now
        where metric.concernId = :concernId
        """,
    )
    fun increaseViewCount(@Param("concernId") concernId: Long, @Param("now") now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ConcernMetric metric
        set metric.commentCount = metric.commentCount + 1,
            metric.updatedAt = :now
        where metric.concernId = :concernId
        """,
    )
    fun increaseCommentCount(@Param("concernId") concernId: Long, @Param("now") now: LocalDateTime): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ConcernMetric metric
        set metric.commentCount = metric.commentCount - 1,
            metric.updatedAt = :now
        where metric.concernId = :concernId
          and metric.commentCount > 0
        """,
    )
    fun decreaseCommentCount(@Param("concernId") concernId: Long, @Param("now") now: LocalDateTime): Int
}
