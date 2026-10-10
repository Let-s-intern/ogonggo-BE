package com.ogonggo.userapi.notification.intake.implement.signup

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.user.domain.LetsCareerAuthProvider
import com.ogonggo.userapi.auth.business.UserSignedUpEvent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor
import java.time.LocalDateTime

class UserSignUpAlimTalkEventListenerTest {

    private val appender = Mockito.mock(NotificationAppender::class.java)
    private val taskExecutor = CapturingTaskExecutor()
    private val listener = UserSignUpAlimTalkEventListener(
        UserSignUpAlimTalkPreparer(),
        appender,
        ObjectMapper().findAndRegisterModules(),
        taskExecutor,
    )

    @ParameterizedTest
    @ValueSource(strings = ["01012345678", "010-1234-5678"])
    fun `허용된 번호를 숫자로 정규화해 가입 알림을 공통 테이블에 적재한다`(phoneNumber: String) {
        // given
        val event = signUpEvent().copy(phoneNum = phoneNumber)

        // when
        listener.handle(event)

        // then
        Mockito.verifyNoInteractions(appender)
        taskExecutor.runAll()
        val notification = notification()
        Mockito.verify(appender).appendInNewTransaction(notification)
        assertEquals("sign_up_confirm:user:17:KAKAO", notification.deduplicationKey)
        assertEquals(NotificationChannel.KAKAO, notification.channel)
        assertEquals("sign_up_confirm", notification.templateCode)
        assertEquals("01012345678", notification.recipientAddress)
        assertEquals(17L, notification.recipientUserId)
        assertEquals(event.joinedAt, notification.scheduledAt)
        assertEquals(
            mapOf(
                "name" to "김렛츠",
                "userEmail" to "lets@career.co.kr",
                "loginType" to "이메일 로그인",
                "createDate" to "2026-08-27",
            ),
            ObjectMapper().readValue(notification.payloadJson, Map::class.java),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "", " ", "---", "1", "010", "01112345678", "0101234567", "010123456789",
        "0101234-5678", "010-12345678", "010--1234-5678", "bad010-1234-5678",
        "+821012345678", " 01012345678", "01012345678 ", "010１２３４５６７８",
    ])
    fun `허용하지 않은 번호는 예외 없이 알림 적재만 생략한다`(phoneNumber: String) {
        // given
        val event = signUpEvent().copy(phoneNum = phoneNumber)

        // when
        assertDoesNotThrow { listener.handle(event) }
        taskExecutor.runAll()

        // then
        Mockito.verifyNoInteractions(appender)
    }

    @Test
    fun `필수 정보 누락과 번호 오류의 사유를 민감값 없이 구분해 기록한다`() {
        // given
        val event = signUpEvent()
        val cases = mapOf(
            event.copy(name = null) to "MISSING_NAME",
            event.copy(email = " ") to "MISSING_EMAIL",
            event.copy(phoneNum = null) to "MISSING_PHONE_NUMBER",
            event.copy(phoneNum = "010") to "INVALID_PHONE_NUMBER",
            event.copy(authProvider = null) to "MISSING_AUTH_PROVIDER",
        )
        val logger = LoggerFactory.getLogger(UserSignUpAlimTalkEventListener::class.java) as Logger
        val logAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(logAppender)

        // when / then
        try {
            cases.forEach { (incompleteEvent, reason) ->
                assertDoesNotThrow { listener.handle(incompleteEvent) }
                taskExecutor.runAll()
                assertTrue(logAppender.list.last().formattedMessage.contains("reason=$reason"))
            }
        } finally {
            logger.detachAppender(logAppender)
            logAppender.stop()
        }
        val output = logAppender.list.joinToString { it.formattedMessage }
        listOf("김렛츠", "lets@career.co.kr", "010").forEach { assertFalse(output.contains(it)) }
        Mockito.verifyNoInteractions(appender)
    }

    @Test
    fun `로그인 제공자별 가입 경로 표시명을 알림 변수로 보존한다`() {
        // given
        val loginTypes = mapOf(
            LetsCareerAuthProvider.KAKAO to "카카오톡 로그인",
            LetsCareerAuthProvider.NAVER to "네이버 로그인",
            LetsCareerAuthProvider.GOOGLE to "구글 로그인",
            LetsCareerAuthProvider.SERVICE to "이메일 로그인",
        )

        // when
        loginTypes.forEach { (provider, _) ->
            listener.handle(signUpEvent().copy(authProvider = provider))
            taskExecutor.runAll()
        }

        // then
        loginTypes.values.forEach { loginType -> Mockito.verify(appender).appendInNewTransaction(notification(loginType)) }
    }

    @Test
    fun `같은 가입 사건은 같은 중복키로 적재 요청한다`() {
        // given
        val event = signUpEvent()

        // when
        listener.handle(event)
        listener.handle(event)

        // then
        assertEquals(2, taskExecutor.size)
        taskExecutor.runAll()
        Mockito.verify(appender, Mockito.times(2)).appendInNewTransaction(notification())
    }

    @Test
    fun `알림 적재 실패가 가입 성공 흐름으로 전파되지 않고 기록된다`() {
        // given
        val logger = LoggerFactory.getLogger(UserSignUpAlimTalkEventListener::class.java) as Logger
        val logAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(logAppender)
        Mockito.doThrow(IllegalStateException("DB 장애"))
            .`when`(appender).appendInNewTransaction(notification())

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
        assertTrue(logAppender.list.any { it.level == Level.ERROR && it.throwableProxy != null })
        assertTrue(logAppender.list.any { it.formattedMessage.contains("userId=17") })
    }

    private fun signUpEvent() = UserSignedUpEvent(
        userId = 17L,
        name = "김렛츠",
        email = "lets@career.co.kr",
        phoneNum = "010-1234-5678",
        authProvider = LetsCareerAuthProvider.SERVICE,
        joinedAt = LocalDateTime.of(2026, 8, 27, 0, 0),
    )

    private fun notification(loginType: String = "이메일 로그인") = NotificationAppendDto(
        deduplicationKey = "sign_up_confirm:user:17:KAKAO",
        channel = NotificationChannel.KAKAO,
        templateCode = "sign_up_confirm",
        recipientAddress = "01012345678",
        payloadJson = ObjectMapper().writeValueAsString(
            mapOf(
                "name" to "김렛츠",
                "userEmail" to "lets@career.co.kr",
                "loginType" to loginType,
                "createDate" to "2026-08-27",
            ),
        ),
        scheduledAt = LocalDateTime.of(2026, 8, 27, 0, 0),
        recipientUserId = 17,
    )

    private class CapturingTaskExecutor : TaskExecutor {
        private val tasks = ArrayDeque<Runnable>()

        val size: Int get() = tasks.size

        override fun execute(task: Runnable) {
            tasks.addLast(task)
        }

        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }
    }
}
