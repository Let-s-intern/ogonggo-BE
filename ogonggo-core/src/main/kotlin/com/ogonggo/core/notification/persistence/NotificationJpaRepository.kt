package com.ogonggo.core.notification.persistence

import com.ogonggo.core.notification.domain.Notification
import com.ogonggo.core.notification.domain.NotificationStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface NotificationJpaRepository : JpaRepository<Notification, Long> {

    fun countByStatus(status: NotificationStatus): Long

    fun existsByDeduplicationKey(deduplicationKey: String): Boolean

    /** 적재 시 중복 키를 미리 거르는 조회. 동시 삽입의 최종 방어는 DB 유일 제약이다. */
    fun findAllByDeduplicationKeyIn(deduplicationKeys: Collection<String>): List<Notification>

    /** 마감이 변경된 공고의 아직 발송하지 않은 알림을 제거한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from Notification notification
        where notification.deduplicationKey like concat(:prefix, '%')
          and notification.status = :pending
        """,
    )
    fun deletePendingByDeduplicationKeyPrefix(
        @Param("prefix") prefix: String,
        @Param("pending") pending: NotificationStatus,
    ): Int

    /** 예정 시각이 도래한 대기 알림만 작은 묶음으로 읽는다. */
    @Query(
        """
        select notification from Notification notification
        where notification.scheduledAt <= :now
          and notification.scheduledAt > :notBefore
          and notification.status = :pending
        order by notification.scheduledAt, notification.id
        """,
    )
    fun findDue(
        @Param("now") now: LocalDateTime,
        @Param("notBefore") notBefore: LocalDateTime,
        @Param("pending") pending: NotificationStatus,
        pageable: Pageable,
    ): List<Notification>

    @Query(
        """
        select count(notification) from Notification notification
        where notification.status = :pending
          and notification.scheduledAt <= :now
          and notification.scheduledAt > :notBefore
        """,
    )
    fun countDuePending(
        @Param("now") now: LocalDateTime,
        @Param("notBefore") notBefore: LocalDateTime,
        @Param("pending") pending: NotificationStatus,
    ): Long

    @Query(
        """
        select count(notification) from Notification notification
        where notification.status = :pending
          and notification.scheduledAt <= :notBefore
        """,
    )
    fun countExpiredPending(
        @Param("notBefore") notBefore: LocalDateTime,
        @Param("pending") pending: NotificationStatus,
    ): Long

    @Query(
        """
        select count(notification) from Notification notification
        where notification.status = :pending
          and notification.scheduledAt > :now
        """,
    )
    fun countFuturePending(
        @Param("now") now: LocalDateTime,
        @Param("pending") pending: NotificationStatus,
    ): Long

    /** 발송 완료·실패 후 30일 이상 지난 행만 한 페이지씩 찾는다. */
    @Query(
        """
        select n.id from Notification n
        where n.status in :statuses and n.updatedAt <= :cutoff
        order by n.updatedAt, n.id
        """,
    )
    fun findExpiredTerminalIds(
        @Param("statuses") statuses: Collection<NotificationStatus>,
        @Param("cutoff") cutoff: LocalDateTime,
        pageable: Pageable,
    ): List<Long>
}
