package com.ogonggo.core.notification.delivery.implement

import com.ogonggo.core.notification.domain.Notification
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.domain.NotificationProvider
import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Component
class NotificationManager internal constructor(
    private val notificationRepository: NotificationJpaRepository,
    private val processingPolicy: NotificationProcessingPolicy,
) {

    /** 운영 조회용이며 실제 발송은 반드시 claimDue로 PROCESSING 선점 후 시작한다. */
    @Transactional(readOnly = true)
    fun findDue(
        now: LocalDateTime,
        limit: Int,
    ): List<NotificationMessageDto> {
        require(limit > 0)
        return notificationRepository.findDue(
            now = now,
            pending = NotificationStatus.PENDING,
            pageable = PageRequest.of(0, limit),
        ).map { it.toMessageDto() }
    }

    /** due 행을 attempt token으로 원자적으로 선점하고, 트랜잭션 종료 후 caller가 provider를 호출한다. */
    @Transactional
    fun claimDue(now: LocalDateTime, limit: Int): List<NotificationMessageDto> {
        require(limit > 0)
        val candidates = notificationRepository.findDue(
            now = now,
            pending = NotificationStatus.PENDING,
            pageable = PageRequest.of(0, limit),
        )
        return candidates.mapNotNull { notification ->
            val attemptId = UUID.randomUUID().toString()
            val id = checkNotNull(notification.id)
            val claimed = notificationRepository.claimPending(
                id = id,
                pending = NotificationStatus.PENDING,
                processing = NotificationStatus.PROCESSING,
                attemptId = attemptId,
                now = now,
            )
            if (claimed == 1) notification.toMessageDto(attemptId) else null
        }
    }

    /** provider 호출 전에 실행한다. NHN만 최초 요청 기준 멱등성 창 안에서 한 번 재시도한다. */
    @Transactional
    fun recoverStaleProcessing(now: LocalDateTime, limitPerProvider: Int): Int {
        require(limitPerProvider > 0)
        var recovered = 0

        NotificationProvider.entries.forEach { provider ->
            val settings = processingPolicy.settings(provider)
            val stale = notificationRepository.findStaleProcessing(
                processing = NotificationStatus.PROCESSING,
                channel = provider.channel,
                staleBefore = now.minus(settings.staleTimeout),
                pageable = PageRequest.of(0, limitPerProvider),
            )
            stale.forEach { notification ->
                val id = checkNotNull(notification.id)
                val attemptId = notification.processingAttemptId
                val retryWithinProviderGuarantee =
                    attemptId != null &&
                        settings.staleAction == NotificationStaleAction.RETRY_WITH_IDEMPOTENCY &&
                        notification.attemptCount <= settings.maxRecoveryRetries &&
                        notification.firstAttemptAt?.plus(settings.idempotencyWindow!!)?.isAfter(now) == true &&
                        (notification.dispatchExpiresAt == null || notification.dispatchExpiresAt.isAfter(now))

                if (retryWithinProviderGuarantee) {
                    recovered += notificationRepository.requeueStaleProcessing(
                        id = id,
                        processing = NotificationStatus.PROCESSING,
                        pending = NotificationStatus.PENDING,
                        attemptId = checkNotNull(attemptId),
                        now = now,
                    )
                } else {
                    recovered += notificationRepository.markStaleProcessingUnknown(
                        id = id,
                        processing = NotificationStatus.PROCESSING,
                        unknown = NotificationStatus.UNKNOWN,
                        attemptId = attemptId,
                        resultCode = STALE_PROCESSING_RESULT_CODE,
                        failureCategory = NotificationFailureCategory.PROCESSING_TIMEOUT,
                        detail = "provider 처리 시작 후 ${settings.staleTimeout.seconds}초 동안 결과가 저장되지 않았습니다.",
                        now = now,
                    )
                }
            }
        }
        return recovered
    }

    /** executor가 작업을 받지 않은 경우에만 대기로 돌린다. attempt 횟수와 최초 시각은 감사 목적으로 유지한다. */
    @Transactional
    fun releaseClaim(notificationId: Long, attemptId: String, now: LocalDateTime): Boolean =
        notificationRepository.releaseUnsentClaim(
            id = notificationId,
            processing = NotificationStatus.PROCESSING,
            pending = NotificationStatus.PENDING,
            attemptId = attemptId,
            now = now,
        ) == 1

    /** Actuator backlog gauge용 PENDING 전체 건수다. */
    @Transactional(readOnly = true)
    fun countPendingTotal(): Long = notificationRepository.countByStatus(NotificationStatus.PENDING)

    @Transactional(readOnly = true)
    fun countProcessingTotal(): Long = notificationRepository.countByStatus(NotificationStatus.PROCESSING)

    @Transactional(readOnly = true)
    fun countStaleProcessing(now: LocalDateTime): Long = NotificationProvider.entries.sumOf { provider ->
        val timeout = processingPolicy.settings(provider).staleTimeout
        notificationRepository.countStaleProcessing(
            processing = NotificationStatus.PROCESSING,
            channel = provider.channel,
            staleBefore = now.minus(timeout),
        )
    }

    /** SENT뿐 아니라 FAILED·UNKNOWN 저장도 전역 진행으로 본다. */
    fun hasDeliveryProgressSince(since: LocalDateTime): Boolean =
        notificationRepository.existsByStatusInAndUpdatedAtGreaterThanEqual(
            listOf(NotificationStatus.SENT, NotificationStatus.FAILED, NotificationStatus.UNKNOWN), since,
        )

    @Transactional(readOnly = true)
    fun countPendingDue(now: LocalDateTime): Long =
        notificationRepository.countDuePending(now, NotificationStatus.PENDING)

    @Transactional(readOnly = true)
    fun countPendingExpired(now: LocalDateTime): Long =
        notificationRepository.countExpiredPending(now, NotificationStatus.PENDING)

    @Transactional(readOnly = true)
    fun countPendingFuture(now: LocalDateTime): Long =
        notificationRepository.countFuturePending(now, NotificationStatus.PENDING)

    /** 결과 쓰기는 id + attempt token 조건부다. stale UNKNOWN은 새 시도가 없으면 늦은 응답으로 정정할 수 있다. */
    @Transactional
    fun complete(notificationId: Long, attemptId: String, result: NotificationDeliveryResult, now: LocalDateTime): Boolean {
        val status = when (result) {
            is NotificationDeliveryResult.Sent -> NotificationStatus.SENT
            is NotificationDeliveryResult.Failed -> NotificationStatus.FAILED
            is NotificationDeliveryResult.Unknown -> NotificationStatus.UNKNOWN
        }
        val sent = result as? NotificationDeliveryResult.Sent
        val resultCode = when (result) {
            is NotificationDeliveryResult.Sent -> null
            is NotificationDeliveryResult.Failed -> result.resultCode
            is NotificationDeliveryResult.Unknown -> result.resultCode
        }
        val failureCategory = when (result) {
            is NotificationDeliveryResult.Sent -> null
            is NotificationDeliveryResult.Failed -> result.failureCategory
            is NotificationDeliveryResult.Unknown -> result.failureCategory
        }
        val detail = when (result) {
            is NotificationDeliveryResult.Sent -> null
            is NotificationDeliveryResult.Failed -> result.detail
            is NotificationDeliveryResult.Unknown -> result.detail
        }
        return notificationRepository.completeProcessing(
            id = notificationId,
            processing = NotificationStatus.PROCESSING,
            unknown = NotificationStatus.UNKNOWN,
            status = status,
            attemptId = attemptId,
            staleResultCode = STALE_PROCESSING_RESULT_CODE,
            providerMessageId = sent?.providerMessageId,
            sentAt = if (sent == null) null else now,
            resultCode = resultCode,
            failureCategory = failureCategory,
            detail = detail?.trim()?.take(500),
            now = now,
        ) == 1
    }

    /** 공고 마감이 바뀌면 아직 발송을 시작하지 않은 이전 회차만 제거한다. */
    @Transactional
    fun deletePendingByDeduplicationKeyPrefix(prefix: String): Int {
        require(prefix.isNotBlank())
        return notificationRepository.deletePendingByDeduplicationKeyPrefix(
            prefix = prefix,
            pending = NotificationStatus.PENDING,
        )
    }

    private fun Notification.toMessageDto() = NotificationMessageDto(
        notificationId = checkNotNull(id),
        channel = channel,
        recipientType = recipientType,
        templateCode = templateCode,
        recipientAddress = checkNotNull(recipientAddress),
        payloadJson = checkNotNull(payloadJson),
        deduplicationKey = deduplicationKey,
        recipientUserId = recipientUserId,
        userNotificationId = userNotificationId,
        processingAttemptId = processingAttemptId,
    )

    private fun Notification.toMessageDto(attemptId: String) = toMessageDto().copy(processingAttemptId = attemptId)

    private companion object {
        const val STALE_PROCESSING_RESULT_CODE = "STALE_PROCESSING_TIMEOUT"
    }
}
