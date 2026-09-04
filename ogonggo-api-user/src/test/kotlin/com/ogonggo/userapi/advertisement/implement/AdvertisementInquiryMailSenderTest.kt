package com.ogonggo.userapi.advertisement.implement

import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

class AdvertisementInquiryMailSenderTest {

    private val javaMailSender = Mockito.mock(JavaMailSender::class.java)

    @Test
    fun `문의를 남긴 주소로 접수 확인 메일을 보낸다`() {
        val sender = AdvertisementInquiryMailSender(javaMailSender, AdvertisementMailProperties(from = FROM))

        sender.sendConfirmation(NOTIFICATION)

        val captor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        Mockito.verify(javaMailSender).send(captor.capture() ?: SimpleMailMessage())

        val sent = captor.value
        assertEquals(FROM, sent.from)
        assertEquals(listOf("manager@ogonggo.co.kr"), sent.to?.toList())
        assertEquals("[오공고] 광고 문의가 접수되었습니다", sent.subject)

        val body = assertNotNull(sent.text).let { sent.text!! }
        assertTrue(body.contains("김담당님"))
        assertTrue(body.contains("렛츠커리어"))
        assertTrue(body.contains("지금 바로 무료 홍보 할래요"))
        // 문의 본문은 다시 싣지 않는다. 메일함에 남는 사본이라 짧게 유지한다.
        assertTrue(!body.contains(PROMOTION_ANSWER))
    }

    @Test
    fun `발신 주소가 없으면 보내지 않는다`() {
        val sender = AdvertisementInquiryMailSender(javaMailSender, AdvertisementMailProperties(from = ""))

        sender.sendConfirmation(NOTIFICATION)

        Mockito.verifyNoInteractions(javaMailSender)
    }

    @Test
    fun `메일 발송이 실패해도 예외를 밖으로 던지지 않는다`() {
        // 문의는 이미 슬랙으로 전달된 뒤다. 확인 메일 실패로 접수를 되돌리지 않는다.
        val sender = AdvertisementInquiryMailSender(javaMailSender, AdvertisementMailProperties(from = FROM))
        Mockito.doThrow(MailSendException("SMTP 연결 실패"))
            .`when`(javaMailSender).send(Mockito.any(SimpleMailMessage::class.java) ?: SimpleMailMessage())

        sender.sendConfirmation(NOTIFICATION)
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
