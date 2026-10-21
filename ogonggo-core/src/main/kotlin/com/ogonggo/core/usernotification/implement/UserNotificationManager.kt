package com.ogonggo.core.usernotification.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.usernotification.error.UserNotificationErrorCode.USER_NOTIFICATION_NOT_FOUND
import com.ogonggo.core.usernotification.persistence.UserNotificationQueryRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class UserNotificationManager internal constructor(private val repository: UserNotificationQueryRepository) {
    fun markRead(userId: Long, id: Long, since: LocalDateTime, now: LocalDateTime) {
        if (repository.markRead(userId, id, since, now) == 0L && !repository.existsVisible(userId, id, since)) {
            throw EntityNotFoundException(USER_NOTIFICATION_NOT_FOUND)
        }
    }

    fun markAllRead(userId: Long, since: LocalDateTime, now: LocalDateTime) {
        val boundary = repository.findReadBoundary(userId, since, now) ?: return
        repository.markAllRead(userId, boundary, since, now)
    }
}
