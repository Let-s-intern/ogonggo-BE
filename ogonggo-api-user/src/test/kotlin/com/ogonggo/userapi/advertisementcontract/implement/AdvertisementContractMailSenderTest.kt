package com.ogonggo.userapi.advertisementcontract.implement

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.ogonggo.userapi.advertisementcontract.business.AdvertisementContractNotification
import com.ogonggo.userapi.mail.OgonggoMailProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

class AdvertisementContractMailSenderTest {

    private val javaMailSender = Mockito.mock(JavaMailSender::class.java)

    @Test
    @DisplayName("광고 신청 접수 확인 메일에 신청자와 신청 정보를 포함한다")
    fun `신청자 주소로 접수 확인과 신청 카테고리 의미를 보낸다`() {
        // given
        val sender = AdvertisementContractMailSender(javaMailSender, OgonggoMailProperties(from = FROM))

        // when
        sender.sendConfirmation(NOTIFICATION)

        // then
        val captor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        Mockito.verify(javaMailSender, Mockito.times(1)).send(captor.capture())
        val message = captor.value
        assertEquals(FROM, checkNotNull(message.from))
        assertEquals("marketing@example.com", checkNotNull(message.to).single())
        assertEquals("[오공고] 광고 계약 신청이 접수되었습니다", message.subject)
        val body = message.text.orEmpty()
        assertTrue(body.contains("김담당님"))
        assertTrue(body.contains("샘플기업"))
        assertTrue(body.contains("인스타그램 1"))
        assertTrue(body.contains("실제 상품과 일정은 담당자가 확인 후 안내"))
        assertFalse(body.contains("02-1234-5678"))
    }

    @Test
    @DisplayName("발신 주소가 설정되지 않으면 접수 확인 메일을 보내지 않는다")
    fun `발신 주소가 없으면 메일을 보내지 않는다`() {
        // given
        val sender = AdvertisementContractMailSender(javaMailSender, OgonggoMailProperties(from = ""))

        // when
        sender.sendConfirmation(NOTIFICATION)

        // then
        Mockito.verifyNoInteractions(javaMailSender)
    }

    @Test
    @DisplayName("SMTP 발송 실패를 호출자에게 전파하지 않는다")
    fun `SMTP 발송 실패는 접수 확인 메일 호출 밖으로 전파되지 않는다`() {
        // given
        val sender = AdvertisementContractMailSender(javaMailSender, OgonggoMailProperties(from = FROM))
        Mockito.doThrow(MailSendException("SMTP unavailable"))
            .`when`(javaMailSender).send(Mockito.any(SimpleMailMessage::class.java))

        // when
        sender.sendConfirmation(NOTIFICATION)

        // then
        Mockito.verify(javaMailSender, Mockito.times(1)).send(Mockito.any(SimpleMailMessage::class.java))
    }

    @Test
    @DisplayName("SMTP provider 예외의 민감한 원문 없이 오류 stack trace를 기록한다")
    fun `SMTP 예외 로그는 수신 주소와 외부 응답 원문을 숨기고 오류 stack trace를 남긴다`() {
        // given
        val logger = LoggerFactory.getLogger(AdvertisementContractMailSender::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply {
            context = logger.loggerContext
            start()
        }
        logger.addAppender(appender)
        val sender = AdvertisementContractMailSender(javaMailSender, OgonggoMailProperties(from = FROM))
        val providerException = MailSendException(
            "SMTP rejected $SENSITIVE_EMAIL at $SENSITIVE_WEBHOOK; response=$SENSITIVE_BODY",
            RuntimeException("provider body $SENSITIVE_BODY"),
        )
        Mockito.doThrow(providerException)
            .`when`(javaMailSender).send(Mockito.any(SimpleMailMessage::class.java))

        try {
            // when
            sender.sendConfirmation(NOTIFICATION)
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }

        // then
        val event = appender.list.single()
        assertEquals(Level.ERROR, event.level)
        assertTrue(event.formattedMessage.contains("contractId=1001"))
        val throwable = checkNotNull(event.throwableProxy)
        assertEquals("외부 provider 예외 상세 정보는 로그에서 제외했습니다.", throwable.message)
        assertTrue(throwable.stackTraceElementProxyArray.isNotEmpty())
        assertTrue(throwable.cause == null)
        val capturedOutput = event.formattedMessage + throwable.message +
            throwable.stackTraceElementProxyArray.joinToString(separator = "\n")
        listOf(SENSITIVE_EMAIL, SENSITIVE_WEBHOOK, SENSITIVE_BODY).forEach { sensitiveValue ->
            assertFalse(capturedOutput.contains(sensitiveValue))
        }
        Mockito.verify(javaMailSender, Mockito.times(1)).send(Mockito.any(SimpleMailMessage::class.java))
    }

    private companion object {
        const val FROM = "official@ogonggo.co.kr"
        const val SENSITIVE_EMAIL = "private-recipient@example.com"
        const val SENSITIVE_WEBHOOK = "https://hooks.slack.com/services/fake-secret"
        const val SENSITIVE_BODY = "raw-provider-response"
        val NOTIFICATION = AdvertisementContractNotification(
            contractId = 1001L,
            promotionTarget = "채용 공고",
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentDeadline = "2026-12-31",
            purpose = "적합한 지원자 확보",
            categories = listOf("배너", "인스타그램 1"),
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
        )
    }
}
