package com.ogonggo.userapi.notification.channel.email

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.userapi.mail.OgonggoMailProperties
import jakarta.mail.MessagingException
import jakarta.mail.Part
import jakarta.mail.SendFailedException
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertEquals("[오늘의 공고] 가입 선물로 AI 자소서 프롬프트를 보내드려요 🎁", sent.subject)
    }

    @Test
    fun `HTML 본문과 텍스트 본문에 모두 무료 자료 링크를 담는다`() {
        // given
        Mockito.doNothing().`when`(javaMailSender).send(Mockito.any(MimeMessage::class.java))

        // when
        sender.send(notification())

        // then
        // HTML을 막은 메일 클라이언트에서도 자료 링크가 보여야 한다.
        val html = partOf(capturedMessage(), "text/html")
        val text = partOf(capturedMessage(), "text/plain")
        listOf(
            "https://app.notion.com/p/letsintern/AI-3ff5e77cbee1808fb14cc45c5165cc70",
            "https://open.kakao.com/o/gMl9i82g",
            "https://www.instagram.com/letscareer.official",
            "https://www.instagram.com/letscareer.job/",
        ).forEach { link ->
            assertTrue(html.contains(link), link)
            assertTrue(text.contains(link), link)
        }
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

    /** 텍스트와 HTML을 함께 보내면 본문이 multipart 안쪽에 있어 MIME 타입이 맞는 파트를 찾을 때까지 내려간다. */
    private fun partOf(message: MimeMessage, mimeType: String): String {
        fun find(part: Part): String? = when (val content = part.content) {
            is MimeMultipart -> (0 until content.count).firstNotNullOfOrNull { find(content.getBodyPart(it)) }
            is String -> content.takeIf { part.isMimeType(mimeType) }
            else -> null
        }
        // 파트별 Content-Type 헤더는 발송 때 saveChanges에서 채워진다. 발송을 가로챘으므로 직접 부른다.
        message.saveChanges()
        return checkNotNull(find(message)) { "$mimeType 본문이 없습니다." }
    }

    private fun capturedMessage(): MimeMessage {
        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        Mockito.verify(javaMailSender).send(captor.capture())
        return captor.value
    }

    private fun notification(
        templateCode: String = "sign_up_welcome",
        payloadJson: String = "{}",
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
