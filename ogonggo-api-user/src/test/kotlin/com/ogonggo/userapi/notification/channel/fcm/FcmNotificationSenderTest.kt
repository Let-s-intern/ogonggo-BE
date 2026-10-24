package com.ogonggo.userapi.notification.channel.fcm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationRecipientType
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenManager
import com.ogonggo.core.userfcmtoken.implement.UserFcmTokenReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.Mockito
import org.junit.jupiter.api.BeforeEach
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
