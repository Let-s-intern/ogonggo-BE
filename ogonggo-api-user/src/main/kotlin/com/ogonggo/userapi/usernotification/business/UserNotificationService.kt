package com.ogonggo.userapi.usernotification.business

import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastReader
import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastManager
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
    private val broadcasts: NotificationBroadcastReader,
    private val broadcastManager: NotificationBroadcastManager,
) {
    fun list(userId: Long, cursor: String?, size: Int): UserNotificationPageResult {
        val user = userReader.read(userId)
        user.status.requireActive()
        val now = LocalDateTime.now(clock)
        val since = now.minusDays(30)
        val position = UserNotificationCursor.decode(cursor)
        val rows = (reader.readPage(userId, since, position, size + 1) +
            broadcasts.readPage(userId, maxOf(since, user.joinedAt), now, position, size + 1))
            .sortedWith(compareByDescending<com.ogonggo.core.usernotification.implement.dto.UserNotificationDto> { it.createdAt }
                .thenByDescending { it.broadcastId != null }.thenByDescending { it.broadcastId }.thenByDescending { it.id })
        val items = rows.take(size)
        val hasNext = rows.size > size
        val nextCursor = if (hasNext) UserNotificationCursor.encode(items.last()) else null
        return UserNotificationPageResult(items, nextCursor, hasNext)
    }

    fun countUnread(userId: Long): Long {
        val user = userReader.read(userId)
        user.status.requireActive()
        val now = LocalDateTime.now(clock)
        return reader.countUnread(userId, now.minusDays(30)) +
            broadcasts.countUnread(userId, maxOf(now.minusDays(30), user.joinedAt), now)
    }

    @Transactional
    fun markRead(userId: Long, id: Long?) {
        userReader.read(userId).status.requireActive()
        val now = LocalDateTime.now(clock)
        manager.markRead(userId, checkNotNull(id), now.minusDays(30), now)
    }

    @Transactional
    fun markBroadcastRead(userId: Long, batchId: java.util.UUID) {
        val user = userReader.read(userId)
        user.status.requireActive()
        val now = LocalDateTime.now(clock)
        broadcastManager.markRead(userId, batchId.toString(), maxOf(now.minusDays(30), user.joinedAt), now)
    }

    @Transactional
    fun markAllRead(userId: Long) {
        val user = userReader.read(userId)
        user.status.requireActive()
        val now = LocalDateTime.now(clock)
        broadcastManager.markAllRead(userId, maxOf(now.minusDays(30), user.joinedAt), now)
        manager.markAllRead(userId, now.minusDays(30), now)
    }
}
