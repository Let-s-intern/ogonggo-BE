package com.ogonggo.userapi.notification.intake.implement.comment

import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.usernotification.domain.UserNotificationPushSkipReason
import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.dto.UserNotificationAppendDto
import org.springframework.stereotype.Component

/** 표시 스냅샷을 준비해 반환한다. 알림함과 발송 대기열의 저장은 리스너가 담당한다. */
@Component
internal class CommentNotificationPreparer(private val profileReader: UserProfileReader) {
    fun prepare(
        type: UserNotificationType,
        recipientUserId: Long,
        actorUserId: Long,
        postId: Long,
        commentId: Long,
        postTitle: String,
        content: String,
        pushSkipReason: UserNotificationPushSkipReason?,
    ): UserNotificationAppendDto {
        val profile = profileReader.read(actorUserId)
        val nickname = profile?.nickname?.takeIf { it.isNotBlank() } ?: "회원"
        // 두 댓글 도메인의 본문은 일반 텍스트다. 공백을 정리하고 Unicode 문자 단위로 자른다.
        val plainText = content.replace(WHITESPACE, " ").trim()
        val previewLength = minOf(100, plainText.codePointCount(0, plainText.length))
        val preview = plainText.substring(0, plainText.offsetByCodePoints(0, previewLength))
        return UserNotificationAppendDto(
            recipientUserId = recipientUserId,
            type = type,
            actorUserId = actorUserId,
            postId = postId,
            commentId = commentId,
            actorNickname = nickname,
            actorProfileImageUrl = profile?.profileImageUrl?.takeIf { it.isNotBlank() },
            postTitle = postTitle,
            preview = preview,
            pushSkipReason = pushSkipReason,
        )
    }

    private companion object {
        val WHITESPACE = Regex("[\\s\\p{Z}]+")
    }
}
