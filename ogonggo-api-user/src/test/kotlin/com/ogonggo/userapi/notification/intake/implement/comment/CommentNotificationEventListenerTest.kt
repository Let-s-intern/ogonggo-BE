package com.ogonggo.userapi.notification.intake.implement.comment

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenReader
import com.ogonggo.core.usernotification.domain.UserNotificationType
import com.ogonggo.core.usernotification.implement.UserNotificationAppender
import com.ogonggo.core.usernotification.implement.dto.UserNotificationAppendDto
import com.ogonggo.userapi.concern.business.ConcernCommentCreatedEvent
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostCommentCreatedEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class CommentNotificationEventListenerTest {
    private val users = Mockito.mock(UserReader::class.java)
    private val profiles = Mockito.mock(UserProfileReader::class.java)
    private val tokens = Mockito.mock(UserFcmTokenReader::class.java)
    private val deliveries = Mockito.mock(NotificationAppender::class.java)
    private val appender = Mockito.mock(UserNotificationAppender::class.java)
    private val listener = CommentNotificationEventListener(
        users, tokens, CommentNotificationPreparer(profiles), appender, deliveries, jacksonObjectMapper(),
    )

    @Test
    fun `고민 대댓글과 모집글 최상위 댓글의 작성 사실은 알림 대상이 아니다`() {
        // given
        val concernReply = ConcernCommentCreatedEvent(3, 1, "제목", 4, 5, 2, "답글", NOW)
        val postComment = RecruitmentPostCommentCreatedEvent(3, "제목", 4, null, null, 2, "댓글", NOW)

        // when
        listener.handle(concernReply)
        listener.handle(postComment)

        // then
        Mockito.verifyNoInteractions(users, profiles, tokens, appender, deliveries)
    }

    @Test
    fun `적재 실패는 호출자에게 전파하고 로그와 예외에 민감한 원문을 포함하지 않는다`() {
        // given
        Mockito.`when`(users.read(1)).thenReturn(UserAccountDto(1, 1, null, UserStatus.ACTIVE, UserRole.USER, NOW))
        Mockito.`when`(tokens.readActive(1)).thenReturn(emptyList())
        Mockito.doAnswer {
            throw IllegalStateException("secret-token and raw-comment", RuntimeException("secret-payload"))
        }.`when`(appender).append(Mockito.any(UserNotificationAppendDto::class.java) ?: UserNotificationAppendDto(
            1, UserNotificationType.CONCERN_COMMENT, 2, 3, 4, "회원", null, "제목", "미리보기", null,
        ))
        val logger = LoggerFactory.getLogger(CommentNotificationEventListener::class.java) as Logger
        val logs = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(logs)
        try {
            // when
            val exception = assertThrows(InternalServerException::class.java) {
                listener.handle(ConcernCommentCreatedEvent(3, 1, "제목", 4, null, 2, "댓글 원문", NOW))
            }

            // then
            assertEquals("INTERNAL_SERVER_ERROR", exception.errorCode.code)
            assertNull(exception.cause)
            val event = logs.list.single()
            assertTrue(event.formattedMessage.contains("type=CONCERN_COMMENT, postId=3, commentId=4"))
            assertFalse(event.formattedMessage.contains("secret"))
            assertFalse(event.throwableProxy.message.contains("secret"))
            assertNull(event.throwableProxy.cause)
            Mockito.verifyNoInteractions(deliveries)
        } finally {
            logger.detachAppender(logs)
        }
    }

    private companion object { val NOW = LocalDateTime.of(2026, 10, 21, 12, 0) }
}
