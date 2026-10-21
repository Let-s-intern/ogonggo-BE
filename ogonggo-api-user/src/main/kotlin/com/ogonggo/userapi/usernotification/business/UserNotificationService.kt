package com.ogonggo.userapi.usernotification.business

import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.usernotification.implement.UserNotificationManager
import com.ogonggo.core.usernotification.implement.UserNotificationReader
import com.ogonggo.userapi.user.implement.requireActive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserNotificationService(
    private val userReader: UserReader,
    private val reader: UserNotificationReader,
    private val manager: UserNotificationManager,
    private val clock: Clock,
) {
    fun list(userId: Long, cursor: String?, size: Int): UserNotificationPageResult {
        userReader.read(userId).status.requireActive()
        val position = UserNotificationCursor.decode(cursor)
        val rows = reader.readPage(userId, LocalDateTime.now(clock).minusDays(30), position, size + 1)
        val items = rows.take(size)
        val hasNext = rows.size > size
        val nextCursor = if (hasNext) UserNotificationCursor.encode(items.last()) else null
        return UserNotificationPageResult(items, nextCursor, hasNext)
    }

    fun countUnread(userId: Long): Long {
        userReader.read(userId).status.requireActive()
        return reader.countUnread(userId, LocalDateTime.now(clock).minusDays(30))
    }

    @Transactional
    fun markRead(userId: Long, id: Long) {
        userReader.read(userId).status.requireActive()
        val now = LocalDateTime.now(clock)
        manager.markRead(userId, id, now.minusDays(30), now)
    }

    @Transactional
    fun markAllRead(userId: Long) {
        userReader.read(userId).status.requireActive()
        val now = LocalDateTime.now(clock)
        manager.markAllRead(userId, now.minusDays(30), now)
    }
}
