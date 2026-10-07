package com.ogonggo.core.notification.delivery.implement

import com.ogonggo.core.notification.domain.Notification
import com.ogonggo.core.notification.domain.NotificationStatus
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class NotificationManager internal constructor(
    private val notificationRepository: NotificationJpaRepository,
) {

    /** 전역 delivery 스케줄 잠금 안에서 처리할 due 알림을 작은 묶음으로 읽는다. */
    @Transactional(readOnly = true)
    fun findDue(now: LocalDateTime, limit: Int): List<NotificationMessageDto> {
        require(limit > 0)
        return notificationRepository.findDue(
            now = now,
            pending = NotificationStatus.PENDING,
            pageable = PageRequest.of(0, limit),
        ).map { it.toMessageDto() }
    }

    /** provider 응답만 저장한다. 오류는 최종 FAILED로 남기며 자동 재시도하지 않는다. */
    @Transactional
    fun complete(notificationId: Long, result: NotificationDeliveryResult, now: LocalDateTime): Boolean {
        val notification = notificationRepository.findById(notificationId).orElse(null) ?: return false
        if (notification.status != NotificationStatus.PENDING) return false

        when (result) {
            is NotificationDeliveryResult.Sent -> notification.markSent(now, result.providerMessageId)
            is NotificationDeliveryResult.Failed -> notification.markFailed(result.resultCode)
        }
        return true
    }

    /** 공고 마감이 바뀌면 아직 대기 중인 이전 회차만 제거한다. */
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
        templateCode = templateCode,
        recipientAddress = checkNotNull(recipientAddress),
        payloadJson = checkNotNull(payloadJson),
        deduplicationKey = deduplicationKey,
    )
}
