package com.ogonggo.userapi.advertisement.implement

import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryType
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.io.ByteArrayOutputStream

class AdvertisementInquiryMailSenderTest {

    /**
     * MimeMessage 는 세션이 있어야 만들어져 목으로 대신할 수 없다.
     * 실제 구현으로 메시지만 만들고 발송은 스파이로 가로챈다.
     */
    private val javaMailSender = Mockito.spy(JavaMailSenderImpl())

    @Test
    fun `문의를 남긴 주소로 소개서를 붙인 접수 확인 메일을 보낸다`() {
        val sender = AdvertisementInquiryMailSender(javaMailSender, AdvertisementMailProperties(from = FROM))
        Mockito.doNothing().`when`(javaMailSender).send(Mockito.any(MimeMessage::class.java))

        sender.sendConfirmation(NOTIFICATION)

        val sent = capturedMessage()
        assertEquals(FROM, sent.from.single().toString())
        assertEquals("manager@ogonggo.co.kr", sent.allRecipients.single().toString())
        assertEquals("[렛츠커리어] 채용공고 광고 문의가 접수되었습니다", sent.subject)

        // 본문과 첨부가 함께 실려야 한다.
        assertTrue((sent.content as MimeMultipart).count > 1)

        // 한글 파일명은 그대로 실리지 않고 인코딩되므로 확장자와 MIME 타입으로 확인한다.
        val raw = ByteArrayOutputStream().also { sent.writeTo(it) }.toString(Charsets.UTF_8)
        assertTrue(raw.contains("application/pdf"))
        assertTrue(raw.contains(".pdf"))
    }

    @Test
    fun `본문에 담당자 이름과 신청 내용을 담는다`() {
        val sender = AdvertisementInquiryMailSender(javaMailSender, AdvertisementMailProperties(from = FROM))
        Mockito.doNothing().`when`(javaMailSender).send(Mockito.any(MimeMessage::class.java))

        sender.sendConfirmation(NOTIFICATION)

        val body = textOf(capturedMessage())
        assertTrue(body.contains("김담당님"))
        assertTrue(body.contains("렛츠커리어"))
        assertTrue(body.contains("지금 바로 무료 홍보 할래요"))
        assertTrue(body.contains("010-1234-5678"))
        // 문의 본문은 다시 싣지 않는다. 메일함에 남는 사본이라 짧게 유지한다.
        assertTrue(!body.contains(PROMOTION_ANSWER))
    }

    @Test
    fun `발신 주소가 없으면 보내지 않는다`() {
        val sender = AdvertisementInquiryMailSender(javaMailSender, AdvertisementMailProperties(from = ""))

        sender.sendConfirmation(NOTIFICATION)

        Mockito.verify(javaMailSender, Mockito.never()).send(Mockito.any(MimeMessage::class.java))
    }

    @Test
    fun `메일 발송이 실패해도 예외를 밖으로 던지지 않는다`() {
        // 문의는 이미 슬랙으로 전달된 뒤다. 확인 메일 실패로 접수를 되돌리지 않는다.
        val sender = AdvertisementInquiryMailSender(javaMailSender, AdvertisementMailProperties(from = FROM))
        Mockito.doThrow(MailSendException("SMTP 연결 실패"))
            .`when`(javaMailSender).send(Mockito.any(MimeMessage::class.java))

        sender.sendConfirmation(NOTIFICATION)
    }

    /**
     * MimeMessageHelper 는 mixed 안에 related 를 넣고 그 안에 본문을 둔다.
     * 첫 번째 파트가 곧 본문이 아니라서 text/plain 을 찾을 때까지 내려간다.
     */
    private fun textOf(message: MimeMessage): String {
        fun find(content: Any?): String? = when (content) {
            is String -> content
            is MimeMultipart -> (0 until content.count).firstNotNullOfOrNull { find(content.getBodyPart(it).content) }
            else -> null
        }
        return find(message.content) ?: ""
    }

    private fun capturedMessage(): MimeMessage {
        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        Mockito.verify(javaMailSender).send(captor.capture())
        return captor.value
    }

    companion object {
        private const val FROM = "official@letscareer.co.kr"
        private const val PROMOTION_ANSWER = "사람인과 잡코리아에 올리고 있습니다."
        private val NOTIFICATION = AdvertisementInquiryNotification(
            companyName = "렛츠커리어",
            managerName = "김담당",
            email = "manager@ogonggo.co.kr",
            phoneNumber = "010-1234-5678",
            inquiryType = AdvertisementInquiryType.FREE_PROMOTION,
            promotionAnswer = PROMOTION_ANSWER,
        )
    }
}
