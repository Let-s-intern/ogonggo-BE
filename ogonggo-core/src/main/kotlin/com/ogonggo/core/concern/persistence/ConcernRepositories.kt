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

    /** 숨긴 고민글은 사용자에게 없는 글과 같으므로 뺀다. */
    fun findByIdAndHiddenFalseAndDeletedAtIsNull(id: Long): Concern?

    /** 관리자 콘솔은 숨긴 고민글도 읽는다. */
    fun findByIdAndDeletedAtIsNull(id: Long): Concern?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select concern
        from Concern concern
        where concern.id = :concernId
          and concern.hidden = false
          and concern.deletedAt is null
        """,
    )
    fun findActiveByIdForUpdate(@Param("concernId") concernId: Long): Concern?

    /** 관리자 콘솔이 숨긴 고민글을 포함해 여러 건을 잠근다. 교착을 막으려고 식별자 순으로 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select concern
        from Concern concern
        where concern.id in :concernIds
          and concern.deletedAt is null
        order by concern.id
        """,
    )
    fun findAllByIdInForUpdate(@Param("concernIds") concernIds: Collection<Long>): List<Concern>

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
