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
    @field:Schema(description = "댓글 알림의 원글 제목 또는 관리자 메시지 제목") val postTitle: String,
    @field:Schema(description = "댓글 또는 관리자 메시지 본문의 미리보기. 유니코드 최대 100자") val preview: String,
    @field:Schema(nullable = true, description = "댓글 알림의 원글 ID. 관리자 알림은 null") val postId: Long?,
    @field:Schema(nullable = true, description = "댓글 알림의 댓글 ID. 관리자 알림은 null") val commentId: Long?,
    val isRead: Boolean,
    @field:Schema(description = "Asia/Seoul 기준 ISO-8601 시각 (offset 없음)") val createdAt: LocalDateTime,
    @field:Schema(nullable = true, description = "관리자 알림의 이동 URL. 댓글 알림은 null") val url: String?,
) {
    companion object {
        fun from(item: UserNotificationDto) = UserNotificationResponse(
            item.id, item.type, item.actorNickname, item.actorProfileImageUrl, item.postTitle,
            item.preview, item.postId, item.commentId, item.readAt != null, item.createdAt, item.url,
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
