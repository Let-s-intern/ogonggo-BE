package com.ogonggo.core.notificationbatch.implement

import com.ogonggo.core.notificationbatch.persistence.NotificationBroadcastQueryRepository
import com.ogonggo.core.usernotification.implement.dto.UserNotificationCursorDto
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NotificationBroadcastReader internal constructor(private val queries: NotificationBroadcastQueryRepository) {
    fun readPage(userId: Long, since: LocalDateTime, now: LocalDateTime, cursor: UserNotificationCursorDto?, limit: Int) =
        queries.page(userId, since, now, cursor, limit.also { require(it in 1..101) })
    fun countUnread(userId: Long, since: LocalDateTime, now: LocalDateTime) = queries.countUnread(userId, since, now)
}
