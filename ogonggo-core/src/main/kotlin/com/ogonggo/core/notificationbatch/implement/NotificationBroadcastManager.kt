package com.ogonggo.core.notificationbatch.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.notificationbatch.domain.NotificationBroadcastRead
import com.ogonggo.core.notificationbatch.persistence.NotificationBroadcastReadJpaRepository
import com.ogonggo.core.notificationbatch.persistence.NotificationBroadcastQueryRepository
import com.ogonggo.core.usernotification.error.UserNotificationErrorCode.USER_NOTIFICATION_NOT_FOUND
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/** 호출자 트랜잭션에서 회원을 잠가 단건·전체 읽음의 중복 insert를 직렬화한다. */
@Component
class NotificationBroadcastManager internal constructor(
    private val reads: NotificationBroadcastReadJpaRepository,
    private val queries: NotificationBroadcastQueryRepository,
) {
    fun markRead(userId: Long, batchId: String, since: LocalDateTime, now: LocalDateTime) {
        reads.lockUser(userId)
        if (!queries.isVisible(batchId, since, now)) throw EntityNotFoundException(USER_NOTIFICATION_NOT_FOUND)
        if (!reads.existsByUserIdAndBatchId(userId, batchId)) reads.save(NotificationBroadcastRead(userId, batchId, now))
    }

    fun markAllRead(userId: Long, since: LocalDateTime, now: LocalDateTime) {
        reads.lockUser(userId)
        val ids = queries.unreadIds(userId, since, now)
        ids.chunked(500).forEach { chunk ->
            reads.saveAllAndFlush(chunk.map { NotificationBroadcastRead(userId, it, now) })
        }
    }
}
