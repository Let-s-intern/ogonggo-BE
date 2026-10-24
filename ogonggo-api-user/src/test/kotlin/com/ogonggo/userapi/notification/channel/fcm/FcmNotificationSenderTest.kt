package com.ogonggo.userapi.notification.channel.fcm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.ErrorCode
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenManager
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.Mockito
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal class FcmNotificationSenderTest {

    private val firebaseMessaging = Mockito.mock(FirebaseMessaging::class.java)
    private val tokenReader = Mockito.mock(UserFcmTokenReader::class.java)
    private val tokenManager = Mockito.mock(UserFcmTokenManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-21T11:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val sender = FcmNotificationSender(firebaseMessaging, jacksonObjectMapper(), tokenReader, tokenManager, clock)

    @BeforeEach
    fun setUp() {
        Mockito.`when`(tokenReader.isDeliverable(17L, "fcm-token-1")).thenReturn(true)
    }

    @Test
    fun `FCM payload를 Firebase 메시지로 보내고 provider 메시지 ID를 반환한다`() {
        Mockito.`when`(firebaseMessaging.send(Mockito.any(Message::class.java))).thenReturn("message-1")

        val result = sender.send(
            NotificationMessageDto(
                notificationId = 1L,
                channel = NotificationChannel.FCM,
                templateCode = "job_bookmark_reminder",
                recipientAddress = "fcm-token-1",
                payloadJson = """{"title":"마감 임박","body":"공고가 내일 마감됩니다.","data":{"jobId":"7"}}""",
                deduplicationKey = "job-reminder:7:17:FCM",
                recipientUserId = 17L,
            ),
        )

        assertEquals(NotificationDeliveryResult.Sent("message-1"), result)
        Mockito.verify(firebaseMessaging).send(Mockito.any(Message::class.java))
    }

    @Test
    fun `잘못된 FCM payload는 provider를 호출하지 않고 실패한다`() {
        val result = sender.send(
            NotificationMessageDto(
                notificationId = 1L,
                channel = NotificationChannel.FCM,
                templateCode = "job_bookmark_reminder",
                recipientAddress = "fcm-token-1",
                payloadJson = "{}",
                deduplicationKey = "job-reminder:7:17:FCM",
                recipientUserId = 17L,
            ),
        )

        assertEquals("INVALID_FCM_PAYLOAD", (result as NotificationDeliveryResult.Failed).resultCode)
        assertEquals("FCM payload를 해석할 수 없습니다.", result.detail)
        Mockito.verifyNoInteractions(firebaseMessaging)
    }

    @Test
    fun `Firebase 오류 메시지는 민감정보를 제거해 발송 결과에 담는다`() {
        val exception = Mockito.mock(FirebaseMessagingException::class.java)
        Mockito.doReturn(MessagingErrorCode.INVALID_ARGUMENT).`when`(exception).messagingErrorCode
        Mockito.doReturn("Invalid token fcm-token-12345678901234567890:abcdefghijklmnopqrstuvwxyz")
            .`when`(exception).message
        Mockito.`when`(firebaseMessaging.send(Mockito.any(Message::class.java))).thenThrow(exception)

        val result = sender.send(validNotification()) as NotificationDeliveryResult.Failed

        assertEquals("INVALID_ARGUMENT", result.resultCode)
        assertEquals("Invalid token [REDACTED]", result.detail)
        Mockito.verifyNoInteractions(tokenManager)
    }

    @Test
    fun `발송 직전에 소유권이 없거나 회원이 비활성이면 Firebase를 호출하지 않는다`() {
        // given
        Mockito.`when`(tokenReader.isDeliverable(17L, "fcm-token-1")).thenReturn(false)
        // when
        val result = sender.send(validNotification()) as NotificationDeliveryResult.Failed
        // then
        assertEquals("FCM_RECIPIENT_INACTIVE", result.resultCode)
        Mockito.verifyNoInteractions(firebaseMessaging, tokenManager)
    }

    @Test
    fun `회원 식별자 없는 FCM 대기 알림은 발송하지 않는다`() {
        // when
        val result = sender.send(validNotification().copy(recipientUserId = null)) as NotificationDeliveryResult.Failed
        // then
        assertEquals("FCM_RECIPIENT_INACTIVE", result.resultCode)
        Mockito.verifyNoInteractions(firebaseMessaging, tokenManager)
    }

    @Test
    fun `UNREGISTERED 응답은 발송한 회원과 토큰으로 조건부 해제한다`() {
        // given
        val exception = Mockito.mock(FirebaseMessagingException::class.java)
        Mockito.doReturn(MessagingErrorCode.UNREGISTERED).`when`(exception).messagingErrorCode
        Mockito.doReturn("Invalid fcm-token-1").`when`(exception).message
        Mockito.`when`(firebaseMessaging.send(Mockito.any(Message::class.java))).thenThrow(exception)
        // when
        val result = sender.send(validNotification()) as NotificationDeliveryResult.Failed
        // then
        assertEquals("UNREGISTERED", result.resultCode)
        assertEquals("Invalid [REDACTED]", result.detail)
        Mockito.verify(tokenManager).revoke(17L, "fcm-token-1", LocalDateTime.now(clock))
    }

    @ParameterizedTest
    @CsvSource(
        "INVALID_ARGUMENT, INVALID_REQUEST",
        "SENDER_ID_MISMATCH, AUTHENTICATION_CONFIGURATION",
        "THIRD_PARTY_AUTH_ERROR, AUTHENTICATION_CONFIGURATION",
        "QUOTA_EXCEEDED, RATE_LIMITED",
    )
    @DisplayName("FCM이 명시적으로 거절한 요청은 FAILED로 기록하고 토큰을 해제하지 않는다")
    fun `FCM의 명시적 거절을 확정 실패로 분류한다`(
        errorCode: MessagingErrorCode,
        category: NotificationFailureCategory,
    ) {
        // given
        val exception = Mockito.mock(FirebaseMessagingException::class.java)
        Mockito.doReturn(errorCode).`when`(exception).messagingErrorCode
        Mockito.doReturn(ErrorCode.UNKNOWN).`when`(exception).errorCode
        Mockito.`when`(firebaseMessaging.send(Mockito.any(Message::class.java))).thenThrow(exception)

        // when
        val result = sender.send(validNotification())

        // then
        assertEquals(NotificationDeliveryResult.Failed(errorCode.name, category), result)
        Mockito.verifyNoInteractions(tokenManager)
    }

    @ParameterizedTest
    @CsvSource("INTERNAL", "UNAVAILABLE")
    @DisplayName("FCM 서버 오류는 접수 여부를 확정할 수 없어 UNKNOWN으로 기록한다")
    fun `FCM 서버 오류를 미확정으로 분류한다`(errorCode: MessagingErrorCode) {
        // given
        val exception = Mockito.mock(FirebaseMessagingException::class.java)
        Mockito.doReturn(errorCode).`when`(exception).messagingErrorCode
        Mockito.`when`(firebaseMessaging.send(Mockito.any(Message::class.java))).thenThrow(exception)

        // when
        val result = sender.send(validNotification())

        // then
        assertEquals(NotificationDeliveryResult.Unknown(errorCode.name, NotificationFailureCategory.PROVIDER_UNAVAILABLE), result)
        Mockito.verifyNoInteractions(tokenManager)
    }

    @ParameterizedTest
    @CsvSource(
        "DEADLINE_EXCEEDED, TRANSPORT_ERROR",
        "UNAVAILABLE, PROVIDER_UNAVAILABLE",
        "INTERNAL, PROVIDER_UNAVAILABLE",
        "UNKNOWN, UNKNOWN_PROVIDER_ERROR",
        "CANCELLED, TRANSPORT_ERROR",
        "ABORTED, UNKNOWN_PROVIDER_ERROR",
        "DATA_LOSS, UNKNOWN_PROVIDER_ERROR",
    )
    @DisplayName("메시징 코드가 없는 SDK 통신·서버 오류는 공통 코드를 보존하고 UNKNOWN으로 기록한다")
    fun `SDK 공통 오류에서 미확정 원인을 보존한다`(errorCode: ErrorCode, category: NotificationFailureCategory) {
        // given
        val exception = Mockito.mock(FirebaseMessagingException::class.java)
        Mockito.doReturn(errorCode).`when`(exception).errorCode
        Mockito.doReturn("Failed fcm-token-1 private@example.com 01012345678").`when`(exception).message
        Mockito.`when`(firebaseMessaging.send(Mockito.any(Message::class.java))).thenThrow(exception)

        // when
        val result = sender.send(validNotification())

        // then
        assertEquals(NotificationDeliveryResult.Unknown(errorCode.name, category, "Failed [REDACTED] [REDACTED] [REDACTED]"), result)
        Mockito.verifyNoInteractions(tokenManager)
    }

    @ParameterizedTest
    @CsvSource(
        "INVALID_ARGUMENT, INVALID_REQUEST",
        "FAILED_PRECONDITION, INVALID_REQUEST",
        "OUT_OF_RANGE, INVALID_REQUEST",
        "UNAUTHENTICATED, AUTHENTICATION_CONFIGURATION",
        "PERMISSION_DENIED, AUTHENTICATION_CONFIGURATION",
        "RESOURCE_EXHAUSTED, RATE_LIMITED",
        "NOT_FOUND, HTTP_ERROR",
        "CONFLICT, HTTP_ERROR",
        "ALREADY_EXISTS, HTTP_ERROR",
    )
    @DisplayName("메시징 코드가 없는 SDK의 명시적 거절도 FAILED로 기록한다")
    fun `SDK 공통 코드의 명시적 거절을 분류한다`(errorCode: ErrorCode, category: NotificationFailureCategory) {
        // given
        val exception = Mockito.mock(FirebaseMessagingException::class.java)
        Mockito.doReturn(errorCode).`when`(exception).errorCode
        Mockito.`when`(firebaseMessaging.send(Mockito.any(Message::class.java))).thenThrow(exception)

        // when
        val result = sender.send(validNotification())

        // then
        assertEquals(NotificationDeliveryResult.Failed(errorCode.name, category), result)
        Mockito.verifyNoInteractions(tokenManager)
    }

    @Test
    @DisplayName("FCM 예외에 코드가 없으면 HTTP 실패로 추정하지 않고 UNKNOWN으로 기록한다")
    fun `코드 없는 FCM 오류를 미확정으로 분류한다`() {
        // given
        val exception = Mockito.mock(FirebaseMessagingException::class.java)
        Mockito.`when`(firebaseMessaging.send(Mockito.any(Message::class.java))).thenThrow(exception)

        // when
        val result = sender.send(validNotification())

        // then
        assertEquals(NotificationDeliveryResult.Unknown("FIREBASE_MESSAGING_ERROR", NotificationFailureCategory.UNKNOWN_PROVIDER_ERROR), result)
        Mockito.verifyNoInteractions(tokenManager)
    }

    @Test
    @DisplayName("FCM 메시지 생성 단계의 잘못된 토픽은 provider 호출 없이 FAILED로 기록한다")
    fun `유효하지 않은 메시지는 호출 전 실패로 분류한다`() {
        // given
        val notification = validNotification().copy(
            recipientType = NotificationRecipientType.TOPIC,
            recipientUserId = null,
            recipientAddress = "invalid topic?",
        )

        // when
        val result = sender.send(notification)

        // then
        assertEquals(
            NotificationDeliveryResult.Failed(
                "INVALID_FCM_MESSAGE",
                NotificationFailureCategory.INVALID_REQUEST,
                "FCM 메시지 형식이 유효하지 않습니다.",
            ),
            result,
        )
        Mockito.verifyNoInteractions(firebaseMessaging, tokenManager)
    }

    @Test
    @DisplayName("토픽 발송은 회원 소유권 검사 없이 토픽 주소로 요청하고 접수 식별자를 반환한다")
    fun `토픽 대상으로 발송한다`() {
        // given
        var topic: String? = null
        Mockito.doAnswer { call ->
            val json = jacksonObjectMapper().readTree(com.google.gson.Gson().toJson(call.getArgument<Message>(0)))
            topic = json["topic"]?.asText()
            "message-topic"
        }.`when`(firebaseMessaging).send(Mockito.any(Message::class.java))
        val notification = validNotification().copy(recipientUserId = null, recipientAddress = "ogonggo-all-test",
            recipientType = NotificationRecipientType.TOPIC)
        // when
        val result = sender.send(notification)
        // then
        assertEquals(NotificationDeliveryResult.Sent("message-topic"), result)
        assertEquals("ogonggo-all-test", topic)
        Mockito.verifyNoInteractions(tokenReader, tokenManager)
    }

    private fun validNotification() = NotificationMessageDto(
        notificationId = 1L,
        channel = NotificationChannel.FCM,
        templateCode = "job_bookmark_reminder",
        recipientAddress = "fcm-token-1",
        payloadJson = """{"title":"마감 임박","body":"공고가 내일 마감됩니다."}""",
        deduplicationKey = "job-reminder:7:17:FCM",
        recipientUserId = 17L,
    )
}
