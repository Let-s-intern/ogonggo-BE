package com.ogonggo.userapi.notification.channel.email

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.userapi.mail.OgonggoMailProperties
import jakarta.mail.MessagingException
import jakarta.mail.SendFailedException
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl

class EmailNotificationSenderTest {

    /**
     * MimeMessage 는 세션이 있어야 만들어져 목으로 대신할 수 없다.
     * 실제 구현으로 메시지만 만들고 발송은 스파이로 가로챈다.
     */
    private val javaMailSender = Mockito.spy(JavaMailSenderImpl())
    private val sender = EmailNotificationSender(javaMailSender, OgonggoMailProperties(from = FROM), ObjectMapper())

    @Test
    fun `가입 환영 메일을 공통 발신 주소에서 알림 행의 수신 주소로 보낸다`() {
        // given
        Mockito.doNothing().`when`(javaMailSender).send(Mockito.any(MimeMessage::class.java))

        // when
        val result = sender.send(notification())

        // then
        assertTrue(result is NotificationDeliveryResult.Sent)
        val sent = capturedMessage()
        assertEquals(FROM, sent.from.single().toString())
        assertEquals("lets@career.co.kr", sent.allRecipients.single().toString())
        assertEquals("[오공고] 오공고에 오신 것을 환영합니다", sent.subject)
        assertTrue((sent.content as String).startsWith("안녕하세요, 김렛츠님."))
    }

    @Test
    fun `이름이 없으면 이름 없이 인사한다`() {
        // given
        Mockito.doNothing().`when`(javaMailSender).send(Mockito.any(MimeMessage::class.java))

        // when
        sender.send(notification(payloadJson = "{}"))

        // then
        val body = capturedMessage().content as String
        assertTrue(body.startsWith("안녕하세요.\n"))
        assertFalse(body.contains("님."))
    }

    @Test
    fun `발신 주소가 없으면 보내지 않고 채널 미설정으로 실패한다`() {
        // given
        val unconfigured = EmailNotificationSender(javaMailSender, OgonggoMailProperties(from = ""), ObjectMapper())

        // when
        val result = unconfigured.send(notification())

        // then
        assertEquals(
            NotificationDeliveryResult.Failed("MAIL_FROM_NOT_CONFIGURED", NotificationFailureCategory.CHANNEL_NOT_CONFIGURED),
            result,
        )
        Mockito.verify(javaMailSender, Mockito.never()).send(Mockito.any(MimeMessage::class.java))
    }

    @Test
    fun `등록되지 않은 템플릿이나 해석할 수 없는 payload는 보내지 않고 실패한다`() {
        // when
        val unknownTemplate = sender.send(notification(templateCode = "unknown"))
        val invalidPayload = sender.send(notification(payloadJson = "not-json"))

        // then
        assertEquals(NotificationFailureCategory.TEMPLATE_CONFIGURATION, (unknownTemplate as NotificationDeliveryResult.Failed).failureCategory)
        assertEquals(NotificationFailureCategory.INVALID_REQUEST, (invalidPayload as NotificationDeliveryResult.Failed).failureCategory)
        Mockito.verify(javaMailSender, Mockito.never()).send(Mockito.any(MimeMessage::class.java))
    }

    @Test
    fun `SMTP 인증 실패와 수신자 거절은 명시적 실패로 기록한다`() {
        // given
        Mockito.doThrow(MailAuthenticationException("인증 실패"))
            .doThrow(MailSendException(mapOf<Any, Exception>(Any() to SendFailedException("수신자 거절"))))
            .`when`(javaMailSender).send(Mockito.any(MimeMessage::class.java))

        // when
        val authentication = sender.send(notification())
        val rejected = sender.send(notification())

        // then
        assertEquals(
            NotificationDeliveryResult.Failed("SMTP_AUTHENTICATION_FAILED", NotificationFailureCategory.AUTHENTICATION_CONFIGURATION),
            authentication,
        )
        assertEquals(NotificationDeliveryResult.Failed("SMTP_REJECTED", NotificationFailureCategory.CLIENT_ERROR), rejected)
    }

    @Test
    fun `접수 여부를 알 수 없는 SMTP 실패는 UNKNOWN으로 기록하고 수신 주소를 남기지 않는다`() {
        // given
        Mockito.doThrow(MailSendException("lets@career.co.kr 전송 중 연결 끊김", MessagingException()))
            .`when`(javaMailSender).send(Mockito.any(MimeMessage::class.java))

        // when
        val result = sender.send(notification())

        // then
        assertEquals(
            NotificationDeliveryResult.Unknown("SMTP_SEND_FAILED", NotificationFailureCategory.TRANSPORT_ERROR),
            result,
        )
    }

    private fun capturedMessage(): MimeMessage {
        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        Mockito.verify(javaMailSender).send(captor.capture())
        return captor.value
    }

    private fun notification(
        templateCode: String = "sign_up_welcome",
        payloadJson: String = """{"name":"김렛츠"}""",
    ) = NotificationMessageDto(
        notificationId = 1L,
        channel = NotificationChannel.EMAIL,
        templateCode = templateCode,
        recipientAddress = "lets@career.co.kr",
        payloadJson = payloadJson,
        deduplicationKey = "sign_up_welcome:user:17:EMAIL",
        recipientUserId = 17L,
    )

    private companion object {
        const val FROM = "official@letscareer.co.kr"
    }
}
