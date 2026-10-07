package com.ogonggo.userapi.notification.fcm.implement

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.user.implement.UserManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito

internal class FcmNotificationSenderTest {

    private val firebaseMessaging = Mockito.mock(FirebaseMessaging::class.java)
    private val userManager = Mockito.mock(UserManager::class.java)
    private val sender = FcmNotificationSender(firebaseMessaging, jacksonObjectMapper(), userManager)

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
        Mockito.verifyNoInteractions(firebaseMessaging)
    }
}
