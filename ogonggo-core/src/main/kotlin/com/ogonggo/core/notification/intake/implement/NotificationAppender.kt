package com.ogonggo.core.notification.intake.implement

import com.ogonggo.core.notification.domain.Notification
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.intake.implement.event.NotificationEnqueuedEvent
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationAppender internal constructor(
    private val notificationRepository: NotificationJpaRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    /** 호출자의 트랜잭션에 참여해 알림 적재를 업무 변경과 함께 커밋한다. */
    @Transactional
    fun append(notification: NotificationAppendDto): Boolean {
        return appendAllInternal(listOf(notification)) == 1
    }

    /** AFTER_COMMIT처럼 호출자 트랜잭션과 분리해 적재해야 하는 경로에서만 사용한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun appendInNewTransaction(notification: NotificationAppendDto): Boolean {
        return appendAllInternal(listOf(notification)) == 1
    }

    @Transactional
    fun appendAll(notifications: Collection<NotificationAppendDto>): Int {
        return appendAllInternal(notifications)
    }

    private fun appendAllInternal(notifications: Collection<NotificationAppendDto>): Int {
        if (notifications.isEmpty()) return 0
        // 잘못된 입력은 DB 쓰기 전에 거부해 불완전한 알림 행이 대기열에 들어오지 않게 한다.
        notifications.forEach {
            require(it.deduplicationKey.isNotBlank())
            require(it.templateCode.isNotBlank())
            require(it.recipientAddress.isNotBlank())
        }

        // 조회는 중복 행 생성을 줄이는 빠른 경로다. 실제 유일성은 DB unique 제약도 보장해야 한다.
        val existingKeys = notificationRepository
            .findAllByDeduplicationKeyIn(notifications.map { it.deduplicationKey }.distinct())
            .mapTo(HashSet()) { it.deduplicationKey }
        // 입력 배치 안의 같은 키도 한 건으로 줄인 뒤 이미 적재된 키를 제외한다.
        val newNotifications = notifications.asSequence()
            .distinctBy { it.deduplicationKey }
            .filterNot { it.deduplicationKey in existingKeys }
            .map {
                Notification(
                    deduplicationKey = it.deduplicationKey,
                    channel = it.channel,
                    templateCode = it.templateCode,
                    recipientUserId = it.recipientUserId,
                    recipientAddress = it.recipientAddress,
                    payloadJson = it.payloadJson,
                    scheduledAt = it.scheduledAt,
                )
            }
            .toList()

        if (newNotifications.isEmpty()) return 0

        try {
            // Flush here so a concurrent unique-key violation is logged before the transaction exits.
            notificationRepository.saveAllAndFlush(newNotifications)
        } catch (exception: DataIntegrityViolationException) {
            if (exception.isDeduplicationKeyConflict()) {
                log.warn(
                    "알림 배치 적재가 deduplication_key 고유 제약 경합으로 롤백됩니다. constraint={}, batchSize={}",
                    DEDUPLICATION_KEY_CONSTRAINT,
                    newNotifications.size,
                )
            }
            throw exception
        }

        newNotifications.groupingBy(Notification::channel).eachCount().forEach { (channel, count) ->
            eventPublisher.publishEvent(NotificationEnqueuedEvent(channel, count))
        }
        return newNotifications.size
    }

    private fun DataIntegrityViolationException.isDeduplicationKeyConflict(): Boolean {
        var cause: Throwable? = this
        while (cause != null) {
            if (cause.message?.contains(DEDUPLICATION_KEY_CONSTRAINT, ignoreCase = true) == true) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    private companion object {
        const val DEDUPLICATION_KEY_CONSTRAINT = "uk_notification_deduplication_key"
        val log = LoggerFactory.getLogger(NotificationAppender::class.java)
    }
}
