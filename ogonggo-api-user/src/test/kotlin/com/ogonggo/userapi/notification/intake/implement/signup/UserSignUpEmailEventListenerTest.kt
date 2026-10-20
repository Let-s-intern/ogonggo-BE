package com.ogonggo.userapi.notification.intake.implement.signup

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.user.domain.LetsCareerAuthProvider
import com.ogonggo.userapi.auth.business.UserSignedUpEvent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor
import java.time.LocalDateTime

class UserSignUpEmailEventListenerTest {

    private val appender = Mockito.mock(NotificationAppender::class.java)
    private val taskExecutor = CapturingTaskExecutor()
    private val listener = UserSignUpEmailEventListener(appender, ObjectMapper(), taskExecutor)

    @Test
    fun `가입한 일반 회원의 렛츠커리어 이메일로 환영 메일을 적재한다`() {
        // when
        listener.handle(signUpEvent())

        // then
        Mockito.verifyNoInteractions(appender)
        taskExecutor.runAll()
        Mockito.verify(appender).appendInNewTransaction(notification("""{"name":"김렛츠"}"""))
    }

    @Test
    fun `휴대폰 번호나 이름이 없어도 이메일이 있으면 적재한다`() {
        // when
        listener.handle(signUpEvent().copy(name = " ", phoneNum = null, authProvider = null))
        taskExecutor.runAll()

        // then
        Mockito.verify(appender).appendInNewTransaction(notification("{}"))
    }

    @Test
    fun `이메일이 없으면 주소 원문 없이 사유만 남기고 적재를 생략한다`() {
        // given
        val logger = LoggerFactory.getLogger(UserSignUpEmailEventListener::class.java) as Logger
        val logAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(logAppender)

        // when
        try {
            assertDoesNotThrow {
                listener.handle(signUpEvent().copy(email = " "))
                taskExecutor.runAll()
            }
        } finally {
            logger.detachAppender(logAppender)
            logAppender.stop()
        }

        // then
        assertTrue(logAppender.list.last().formattedMessage.contains("reason=MISSING_EMAIL"))
        Mockito.verifyNoInteractions(appender)
    }

    @Test
    fun `적재 실패가 가입 흐름으로 전파되지 않고 수신 주소 없이 기록된다`() {
        // given
        val logger = LoggerFactory.getLogger(UserSignUpEmailEventListener::class.java) as Logger
        val logAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(logAppender)
        Mockito.doThrow(IllegalStateException("lets@career.co.kr 적재 실패"))
            .`when`(appender).appendInNewTransaction(notification("""{"name":"김렛츠"}"""))

        // when
        try {
            assertDoesNotThrow {
                listener.handle(signUpEvent())
                taskExecutor.runAll()
            }
        } finally {
            logger.detachAppender(logAppender)
            logAppender.stop()
        }

        // then
        assertTrue(logAppender.list.any { it.formattedMessage.contains("userId=17") })
        val output = logAppender.list.joinToString { it.formattedMessage + (it.throwableProxy?.message ?: "") }
        assertFalse(output.contains("lets@career.co.kr"))
    }

    private fun signUpEvent() = UserSignedUpEvent(
        userId = 17L,
        name = "김렛츠",
        email = "lets@career.co.kr",
        phoneNum = "010-1234-5678",
        authProvider = LetsCareerAuthProvider.SERVICE,
        joinedAt = LocalDateTime.of(2026, 8, 27, 0, 0),
    )

    private fun notification(payloadJson: String) = NotificationAppendDto(
        deduplicationKey = "sign_up_welcome:user:17:EMAIL",
        channel = NotificationChannel.EMAIL,
        templateCode = "sign_up_welcome",
        recipientAddress = "lets@career.co.kr",
        payloadJson = payloadJson,
        scheduledAt = LocalDateTime.of(2026, 8, 27, 0, 0),
        recipientUserId = 17,
    )

    private class CapturingTaskExecutor : TaskExecutor {
        private val tasks = ArrayDeque<Runnable>()

        override fun execute(task: Runnable) {
            tasks.addLast(task)
        }

        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }
    }
}
