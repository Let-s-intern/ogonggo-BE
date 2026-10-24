package com.ogonggo.core.usernotification.implement.dto

import com.ogonggo.core.usernotification.domain.UserNotificationPushSkipReason
import com.ogonggo.core.usernotification.domain.UserNotificationType
import java.time.LocalDateTime

data class UserNotificationAppendDto(
    val recipientUserId: Long,
    val type: UserNotificationType,
    val actorUserId: Long,
    val postId: Long?,
    val commentId: Long?,
    val actorNickname: String,
    val actorProfileImageUrl: String?,
    val postTitle: String,
    val preview: String,
    val pushSkipReason: UserNotificationPushSkipReason?,
    val batchId: String? = null,
    val url: String? = null,
)

data class UserNotificationCursorDto(val createdAt: LocalDateTime, val id: Long)

data class UserNotificationDto(
    val id: Long,
    val type: UserNotificationType,
    val actorNickname: String,
    val actorProfileImageUrl: String?,
    val postTitle: String,
    val preview: String,
    val postId: Long?,
    val commentId: Long?,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val url: String? = null,
)
