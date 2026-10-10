package com.ogonggo.userapi.notification.fcm.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class FcmTestNotificationServiceTest {

    private val fcmTokenService = Mockito.mock(FcmTokenService::class.java)
    private val notificationAppender = Mockito.mock(NotificationAppender::class.java)
    private lateinit var service: FcmTestNotificationService

    @BeforeEach
    fun setUp() {
        service = FcmTestNotificationService(
            fcmTokenService = fcmTokenService,
            notificationAppender = notificationAppender,
            objectMapper = ObjectMapper(),
            clock = Clock.fixed(Instant.parse("2026-10-10T01:02:03Z"), ZoneId.of("Asia/Seoul")),
        )
    }

    @Test
    fun `내 FCM 토큰으로 발송할 notification을 즉시 적재한다`() {
        // given
        Mockito.`when`(fcmTokenService.get(USER_ID)).thenReturn(FCM_TOKEN)
        val command = SendFcmTestNotificationCommand(
            title = "테스트 제목",
            body = "테스트 본문",
            data = mapOf("source" to "manual-test"),
        )

        // when
        val deduplicationKey = service.send(USER_ID, command)

        // then
        assertTrue(deduplicationKey.startsWith("fcm-test:$USER_ID:"))
        val invocation = Mockito.mockingDetails(notificationAppender).invocations.single()
        assertEquals("append", invocation.method.name)
        val notification = invocation.arguments.single() as NotificationAppendDto
        assertEquals(NotificationChannel.FCM, notification.channel)
        assertEquals("fcm_test", notification.templateCode)
        assertEquals(USER_ID, notification.recipientUserId)
        assertEquals(FCM_TOKEN, notification.recipientAddress)
        assertEquals(LocalDateTime.of(2026, 10, 10, 10, 2, 3), notification.scheduledAt)
        assertEquals(
            mapOf(
                "title" to "테스트 제목",
                "body" to "테스트 본문",
                "data" to mapOf("source" to "manual-test"),
            ),
            ObjectMapper().readValue(notification.payloadJson, Map::class.java),
        )
    }

    @Test
    fun `내 FCM 토큰이 없으면 notification을 적재하지 않는다`() {
        // given
        Mockito.`when`(fcmTokenService.get(USER_ID)).thenReturn(null)

        // when
        val exception = assertThrows<EntityNotFoundException> {
            service.send(
                USER_ID,
                SendFcmTestNotificationCommand(
                    title = "테스트 제목",
                    body = "테스트 본문",
                    data = emptyMap(),
                ),
            )
        }

        // then
        assertEquals("FCM_TOKEN_NOT_REGISTERED", exception.errorCode.code)
        Mockito.verifyNoInteractions(notificationAppender)
    }

    companion object {
        private const val USER_ID = 17L
        private const val FCM_TOKEN = "fcm-token-for-test"
    }
}
