package com.ogonggo.core.notification.persistence

import com.ogonggo.core.notification.domain.Notification
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryRecordDto
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface NotificationJpaRepository : JpaRepository<Notification, Long> {

    fun countByStatus(status: NotificationStatus): Long

    /** 정리 인덱스(status, updated_at, id)로 전체 인스턴스의 최근 확정 처리를 확인한다. */
    fun existsByStatusInAndUpdatedAtGreaterThanEqual(statuses: Collection<NotificationStatus>, since: LocalDateTime): Boolean

    fun existsByDeduplicationKey(deduplicationKey: String): Boolean

    /** 적재 시 중복 키를 미리 거르는 조회. 동시 삽입의 최종 방어는 DB 유일 제약이다. */
    fun findAllByDeduplicationKeyIn(deduplicationKeys: Collection<String>): List<Notification>

    /** 운영 결과 화면에 노출할 최신 기록만 DTO projection으로 읽어 민감한 주소·payload 로딩을 피한다. */
    @Query(
        """
        select new com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryRecordDto(
            n.id, n.channel, n.status, n.recipientType, n.templateCode,
            n.recipientUserId, n.userNotificationId, n.batchId, n.scheduledAt,
            n.processingStartedAt, n.sentAt, n.providerMessageId, n.resultCode,
            n.resultCategory, n.resultDetail, n.attemptCount, n.createdAt, n.updatedAt
        )
        from Notification n
        order by n.createdAt desc, n.id desc
        """,
    )
    fun findRecentResults(pageable: Pageable): List<NotificationDeliveryRecordDto>

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
          and (notification.dispatchExpiresAt is null or notification.dispatchExpiresAt > :now)
          and notification.status = :pending
        order by case when notification.batchId is null then 0 else 1 end,
          notification.scheduledAt, notification.id
        """,
    )
    fun findDue(
        @Param("now") now: LocalDateTime,
        @Param("pending") pending: NotificationStatus,
        pageable: Pageable,
    ): List<Notification>

    /** 읽은 PENDING을 attempt token으로 원자적으로 선점한다. 네트워크 호출 전 별도 트랜잭션으로 커밋된다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Notification n
        set n.status = :processing,
            n.processingAttemptId = :attemptId,
            n.processingStartedAt = :now,
            n.firstAttemptAt = coalesce(n.firstAttemptAt, :now),
            n.attemptCount = n.attemptCount + 1,
            n.updatedAt = :now
        where n.id = :id and n.status = :pending
          and n.scheduledAt <= :now
          and (n.dispatchExpiresAt is null or n.dispatchExpiresAt > :now)
        """,
    )
    fun claimPending(
        @Param("id") id: Long,
        @Param("pending") pending: NotificationStatus,
        @Param("processing") processing: NotificationStatus,
        @Param("attemptId") attemptId: String,
        @Param("now") now: LocalDateTime,
    ): Int

    @Query(
        """
        select n from Notification n
        where n.status = :processing and n.channel = :channel
          and n.processingStartedAt <= :staleBefore
        order by n.processingStartedAt, n.id
        """,
    )
    fun findStaleProcessing(
        @Param("processing") processing: NotificationStatus,
        @Param("channel") channel: NotificationChannel,
        @Param("staleBefore") staleBefore: LocalDateTime,
        pageable: Pageable,
    ): List<Notification>

    @Query(
        """
        select count(n) from Notification n
        where n.status = :processing and n.channel = :channel
          and n.processingStartedAt <= :staleBefore
        """,
    )
    fun countStaleProcessing(
        @Param("processing") processing: NotificationStatus,
        @Param("channel") channel: NotificationChannel,
        @Param("staleBefore") staleBefore: LocalDateTime,
    ): Long

    /** 재시도는 기존 firstAttemptAt과 배달 기한을 보존한다. 늦은 provider 응답은 이전 attempt token으로 차단된다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Notification n
        set n.status = :pending, n.processingAttemptId = null,
            n.processingStartedAt = null, n.updatedAt = :now
        where n.id = :id and n.status = :processing and n.processingAttemptId = :attemptId
        """,
    )
    fun requeueStaleProcessing(
        @Param("id") id: Long,
        @Param("processing") processing: NotificationStatus,
        @Param("pending") pending: NotificationStatus,
        @Param("attemptId") attemptId: String,
        @Param("now") now: LocalDateTime,
    ): Int

    /** executor reject는 provider 호출 전이므로 attempt 시각과 횟수를 되돌린다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Notification n
        set n.status = :pending, n.processingAttemptId = null,
            n.processingStartedAt = null,
            n.firstAttemptAt = case when n.attemptCount <= 1 then null else n.firstAttemptAt end,
            n.attemptCount = case when n.attemptCount > 0 then n.attemptCount - 1 else 0 end,
            n.updatedAt = :now
        where n.id = :id and n.status = :processing and n.processingAttemptId = :attemptId
        """,
    )
    fun releaseUnsentClaim(
        @Param("id") id: Long,
        @Param("processing") processing: NotificationStatus,
        @Param("pending") pending: NotificationStatus,
        @Param("attemptId") attemptId: String,
        @Param("now") now: LocalDateTime,
    ): Int

    /** stale 시간 초과는 provider 실패 증거가 아니므로 보수적으로 UNKNOWN으로 종료한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Notification n
        set n.status = :unknown, n.resultCode = :resultCode,
            n.resultCategory = :failureCategory, n.resultDetail = :detail,
            n.processingStartedAt = null, n.updatedAt = :now
        where n.id = :id and n.status = :processing
          and ((:attemptId is null and n.processingAttemptId is null) or n.processingAttemptId = :attemptId)
        """,
    )
    fun markStaleProcessingUnknown(
        @Param("id") id: Long,
        @Param("processing") processing: NotificationStatus,
        @Param("unknown") unknown: NotificationStatus,
        @Param("attemptId") attemptId: String?,
        @Param("resultCode") resultCode: String,
        @Param("failureCategory") failureCategory: NotificationFailureCategory,
        @Param("detail") detail: String,
        @Param("now") now: LocalDateTime,
    ): Int

    /** 결과 업데이트도 상태와 attempt token을 조건으로 하므로 stale 복구와 늦은 응답이 경합하지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Notification n
        set n.status = :status, n.providerMessageId = :providerMessageId,
            n.sentAt = :sentAt, n.resultCode = :resultCode,
            n.resultCategory = :failureCategory, n.resultDetail = :detail,
            n.processingAttemptId = null, n.processingStartedAt = null, n.updatedAt = :now
        where n.id = :id and n.processingAttemptId = :attemptId
          and (n.status = :processing or (n.status = :unknown and n.resultCode = :staleResultCode))
        """,
    )
    fun completeProcessing(
        @Param("id") id: Long,
        @Param("processing") processing: NotificationStatus,
        @Param("unknown") unknown: NotificationStatus,
        @Param("status") status: NotificationStatus,
        @Param("attemptId") attemptId: String,
        @Param("staleResultCode") staleResultCode: String,
        @Param("providerMessageId") providerMessageId: String?,
        @Param("sentAt") sentAt: LocalDateTime?,
        @Param("resultCode") resultCode: String?,
        @Param("failureCategory") failureCategory: NotificationFailureCategory?,
        @Param("detail") detail: String?,
        @Param("now") now: LocalDateTime,
    ): Int

    @Query(
        """
        select count(notification) from Notification notification
        where notification.status = :pending
          and notification.scheduledAt <= :now
          and (notification.dispatchExpiresAt is null or notification.dispatchExpiresAt > :now)
        """,
    )
    fun countDuePending(
        @Param("now") now: LocalDateTime,
        @Param("pending") pending: NotificationStatus,
    ): Long

    @Query(
        """
        select count(notification) from Notification notification
        where notification.status = :pending
          and notification.dispatchExpiresAt <= :now
        """,
    )
    fun countExpiredPending(
        @Param("now") now: LocalDateTime,
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
