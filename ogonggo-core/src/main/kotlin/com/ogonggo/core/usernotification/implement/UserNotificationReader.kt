package com.ogonggo.core.usernotification.implement

import com.ogonggo.core.usernotification.implement.dto.UserNotificationCursorDto
import com.ogonggo.core.usernotification.implement.dto.UserNotificationDto
import com.ogonggo.core.usernotification.persistence.UserNotificationQueryRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class UserNotificationReader internal constructor(private val repository: UserNotificationQueryRepository) {
    fun readPage(userId: Long, since: LocalDateTime, cursor: UserNotificationCursorDto?, limit: Int): List<UserNotificationDto> {
        require(limit in 1..101)
        return repository.findPage(userId, since, cursor, limit).map {
            UserNotificationDto(
                id = checkNotNull(it.id), type = it.type, actorNickname = it.actorNickname,
                actorProfileImageUrl = it.actorProfileImageUrl, postTitle = it.postTitle,
                preview = it.preview, postId = it.postId, commentId = it.commentId,
                readAt = it.readAt, createdAt = it.createdAt,
                url = it.url,
            )
        }
    }

    fun countUnread(userId: Long, since: LocalDateTime): Long = repository.countUnread(userId, since)
}
