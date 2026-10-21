package com.ogonggo.userapi.usernotification.presentation.response

import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.dto.UserNotificationDto
import com.ogonggo.userapi.usernotification.business.UserNotificationPageResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserNotificationResponse(
    val id: Long,
    val type: UserNotificationType,
    val actorNickname: String,
    @field:Schema(nullable = true) val actorProfileImageUrl: String?,
    val postTitle: String,
    val preview: String,
    val postId: Long,
    val commentId: Long,
    val isRead: Boolean,
    @field:Schema(description = "Asia/Seoul 기준 ISO-8601 시각 (offset 없음)") val createdAt: LocalDateTime,
) {
    companion object {
        fun from(item: UserNotificationDto) = UserNotificationResponse(
            item.id, item.type, item.actorNickname, item.actorProfileImageUrl, item.postTitle,
            item.preview, item.postId, item.commentId, item.readAt != null, item.createdAt,
        )
    }
}

data class UserNotificationPageResponse(
    val items: List<UserNotificationResponse>,
    @field:Schema(nullable = true) val nextCursor: String?,
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: UserNotificationPageResult) = UserNotificationPageResponse(
            result.items.map(UserNotificationResponse::from), result.nextCursor, result.hasNext,
        )
    }
}

data class UserNotificationUnreadCountResponse(val count: Long)
