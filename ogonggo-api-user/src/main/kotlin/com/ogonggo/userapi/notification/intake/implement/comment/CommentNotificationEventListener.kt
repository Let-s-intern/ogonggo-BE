package com.ogonggo.userapi.notification.intake.implement.comment

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenReader
import com.ogonggo.core.usernotification.domain.UserNotificationPushSkipReason
import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.UserNotificationAppender
import com.ogonggo.userapi.concern.business.ConcernCommentCreatedEvent
import com.ogonggo.userapi.error.UserApiErrorCode
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostCommentCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** 동기 이벤트로 댓글 트랜잭션에 참여한다. 적재 실패를 전파해 댓글과 알림을 함께 롤백한다. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
internal class CommentNotificationEventListener(
    private val userReader: UserReader,
    private val tokenReader: UserFcmTokenReader,
    private val preparer: CommentNotificationPreparer,
    private val userNotificationAppender: UserNotificationAppender,
    private val notificationAppender: NotificationAppender,
    private val objectMapper: ObjectMapper,
) {
    @EventListener
    fun handle(event: ConcernCommentCreatedEvent) {
        if (event.parentId != null) return
        append(
            type = UserNotificationType.CONCERN_COMMENT,
            recipientUserId = event.concernAuthorUserId,
            actorUserId = event.authorUserId,
            postId = event.concernId,
            commentId = event.commentId,
            postTitle = event.concernTitle,
            content = event.content,
            now = event.occurredAt,
        )
    }

    @EventListener
    fun handle(event: RecruitmentPostCommentCreatedEvent) {
        if (event.parentId == null) return
        append(
            type = UserNotificationType.SIDE_STUDY_COMMENT_REPLY,
            recipientUserId = checkNotNull(event.parentAuthorUserId),
            actorUserId = event.authorUserId,
            postId = event.postId,
            commentId = event.commentId,
            postTitle = event.postTitle,
            content = event.content,
            now = event.occurredAt,
        )
    }

    private fun append(
        type: UserNotificationType,
        recipientUserId: Long,
        actorUserId: Long,
        postId: Long,
        commentId: Long,
        postTitle: String,
        content: String,
        now: LocalDateTime,
    ) {
        if (recipientUserId == actorUserId) return
        try {
            if (userReader.read(recipientUserId).status != UserStatus.ACTIVE) return
            val tokens = tokenReader.readActive(recipientUserId)
            val prepared = preparer.prepare(
                type = type,
                recipientUserId = recipientUserId,
                actorUserId = actorUserId,
                postId = postId,
                commentId = commentId,
                postTitle = postTitle,
                content = content,
                pushSkipReason = if (tokens.isEmpty()) UserNotificationPushSkipReason.NO_ACTIVE_TOKEN else null,
            )
            val id = userNotificationAppender.append(prepared) ?: return
            if (tokens.isEmpty()) return

            val title = when (type) {
                UserNotificationType.SIDE_STUDY_COMMENT_REPLY -> "내 댓글에 답글이 달렸어요"
                UserNotificationType.CONCERN_COMMENT -> "내 고민에 댓글이 달렸어요"
                UserNotificationType.ADMIN_NOTIFICATION -> error("댓글 이벤트는 관리자 알림을 생성하지 않습니다.")
            }
            val payload = objectMapper.writeValueAsString(
                mapOf(
                    "title" to title,
                    "body" to "${prepared.actorNickname}: ${prepared.preview}",
                    "data" to mapOf(
                        "notificationId" to id.toString(),
                        "type" to type.name,
                        "postId" to postId.toString(),
                        "commentId" to commentId.toString(),
                    ),
                ),
            )
            notificationAppender.appendAll(
                tokens.map { token ->
                    NotificationAppendDto(
                        deduplicationKey = "user-notification:$id:token:${token.id}",
                        channel = NotificationChannel.FCM,
                        templateCode = type.name,
                        recipientUserId = recipientUserId,
                        recipientAddress = token.token,
                        payloadJson = payload,
                        scheduledAt = now,
                        userNotificationId = id,
                    )
                },
            )
        } catch (exception: RuntimeException) {
            // DB 예외의 메시지와 cause에는 토큰·댓글 원문이 포함될 수 있어 전달하지 않는다.
            val safeTrace = RuntimeException("댓글 알림 적재 실패").apply { stackTrace = exception.stackTrace }
            log.error(
                "댓글 알림 적재 실패. type={}, postId={}, commentId={}, errorType={}",
                type, postId, commentId, exception::class.simpleName, safeTrace,
            )
            throw InternalServerException(UserApiErrorCode.INTERNAL_SERVER_ERROR)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(CommentNotificationEventListener::class.java)
    }
}
