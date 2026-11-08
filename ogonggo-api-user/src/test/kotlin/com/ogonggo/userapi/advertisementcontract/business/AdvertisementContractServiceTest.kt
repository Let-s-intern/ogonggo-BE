package com.ogonggo.userapi.advertisementcontract.business

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.advertisementcontract.implement.AdvertisementContractMailSender
import com.ogonggo.userapi.advertisementcontract.implement.AdvertisementContractSlackNotifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskRejectedException
import org.springframework.web.client.RestClientException
import java.time.LocalDate

class AdvertisementContractServiceTest {

    @Test
    @DisplayName("토큰 회원 행이 있으면 해당 회원에 연결해 계약을 접수한다")
    fun `토큰 회원 행이 있으면 해당 회원에 연결해 계약을 접수한다`() {
        // given
        val userReader = Mockito.mock(UserReader::class.java)
        val appender = Mockito.mock(AdvertisementContractAppender::class.java)
        val notifier = Mockito.mock(AdvertisementContractSlackNotifier::class.java)
        val mailer = Mockito.mock(AdvertisementContractMailSender::class.java)
        val appendedDtos = mutableListOf<AdvertisementContractFormAppendDto>()
        Mockito.`when`(userReader.readRoles(setOf(USER_ID))).thenReturn(mapOf(USER_ID to UserRole.USER))
        Mockito.doAnswer { invocation ->
            appendedDtos.add(invocation.getArgument<AdvertisementContractFormAppendDto>(0))
            CONTRACT_ID
        }.`when`(appender).appendFromForm(EXPECTED_APPEND_DTO.copy(userId = USER_ID))
        val service = AdvertisementContractService(userReader, appender, notifier, mailer)

        // when
        val result = service.createContract(USER_ID, COMMAND)

        // then
        assertEquals(CONTRACT_ID, result)
        assertEquals(listOf(EXPECTED_APPEND_DTO.copy(userId = USER_ID)), appendedDtos)
        Mockito.verify(appender).appendFromForm(EXPECTED_APPEND_DTO.copy(userId = USER_ID))
    }

    @Test
    @DisplayName("토큰의 회원 행이 없어도 비회원 계약으로 접수한다")
    fun `토큰의 회원 행이 없어도 비회원 계약으로 접수한다`() {
        // given
        val userReader = Mockito.mock(UserReader::class.java)
        val appender = Mockito.mock(AdvertisementContractAppender::class.java)
        val notifier = Mockito.mock(AdvertisementContractSlackNotifier::class.java)
        val mailer = Mockito.mock(AdvertisementContractMailSender::class.java)
        Mockito.`when`(userReader.readRoles(setOf(USER_ID))).thenReturn(emptyMap())
        Mockito.`when`(appender.appendFromForm(EXPECTED_APPEND_DTO)).thenReturn(CONTRACT_ID)
        val service = AdvertisementContractService(userReader, appender, notifier, mailer)

        // when
        val result = service.createContract(USER_ID, COMMAND)

        // then
        assertEquals(CONTRACT_ID, result)
        val order = Mockito.inOrder(appender, notifier, mailer)
        order.verify(appender).appendFromForm(EXPECTED_APPEND_DTO)
        order.verify(notifier).notify(EXPECTED_NOTIFICATION)
        order.verify(mailer).sendConfirmation(EXPECTED_NOTIFICATION)
        Mockito.verify(notifier, Mockito.times(1)).notify(EXPECTED_NOTIFICATION)
        Mockito.verify(mailer, Mockito.times(1)).sendConfirmation(EXPECTED_NOTIFICATION)
        assertNull(EXPECTED_APPEND_DTO.userId)
        assertEquals(setOf(AdvertisementProductCategory.BANNER), EXPECTED_APPEND_DTO.categories)
    }

    @Test
    @DisplayName("Slack 알림 실패에도 계약 접수와 확인 메일을 유지한다")
    fun `Slack 실패가 저장된 신청 접수와 확인 메일을 막지 않는다`() {
        // given
        val userReader = Mockito.mock(UserReader::class.java)
        val appender = Mockito.mock(AdvertisementContractAppender::class.java)
        val notifier = Mockito.mock(AdvertisementContractSlackNotifier::class.java)
        val mailer = Mockito.mock(AdvertisementContractMailSender::class.java)
        Mockito.`when`(appender.appendFromForm(EXPECTED_APPEND_DTO)).thenReturn(CONTRACT_ID)
        Mockito.doThrow(IllegalStateException("Slack webhook unavailable"))
            .`when`(notifier).notify(EXPECTED_NOTIFICATION)
        val service = AdvertisementContractService(userReader, appender, notifier, mailer)

        // when
        assertEquals(CONTRACT_ID, service.createContract(null, COMMAND))

        // then
        Mockito.verify(appender, Mockito.times(1)).appendFromForm(EXPECTED_APPEND_DTO)
        Mockito.verify(notifier, Mockito.times(1)).notify(EXPECTED_NOTIFICATION)
        Mockito.verify(mailer, Mockito.times(1)).sendConfirmation(EXPECTED_NOTIFICATION)
    }

    @Test
    @DisplayName("확인 메일 예약 실패에도 계약 접수 응답을 유지한다")
    fun `확인 메일 비동기 예약 실패가 신청 접수 응답을 바꾸지 않는다`() {
        // given
        val userReader = Mockito.mock(UserReader::class.java)
        val appender = Mockito.mock(AdvertisementContractAppender::class.java)
        val notifier = Mockito.mock(AdvertisementContractSlackNotifier::class.java)
        val mailer = Mockito.mock(AdvertisementContractMailSender::class.java)
        Mockito.`when`(appender.appendFromForm(EXPECTED_APPEND_DTO)).thenReturn(CONTRACT_ID)
        Mockito.doThrow(TaskRejectedException("advertisement executor is full"))
            .`when`(mailer).sendConfirmation(EXPECTED_NOTIFICATION)
        val service = AdvertisementContractService(userReader, appender, notifier, mailer)

        // when
        assertEquals(CONTRACT_ID, service.createContract(null, COMMAND))

        // then
        Mockito.verify(appender, Mockito.times(1)).appendFromForm(EXPECTED_APPEND_DTO)
        Mockito.verify(notifier, Mockito.times(1)).notify(EXPECTED_NOTIFICATION)
        Mockito.verify(mailer, Mockito.times(1)).sendConfirmation(EXPECTED_NOTIFICATION)
    }

    @Test
    @DisplayName("계약 저장 실패 시 후속 알림을 호출하지 않는다")
    fun `계약 저장이 실패하면 알림을 시도하지 않는다`() {
        // given
        val userReader = Mockito.mock(UserReader::class.java)
        val appender = Mockito.mock(AdvertisementContractAppender::class.java)
        val notifier = Mockito.mock(AdvertisementContractSlackNotifier::class.java)
        val mailer = Mockito.mock(AdvertisementContractMailSender::class.java)
        Mockito.`when`(appender.appendFromForm(EXPECTED_APPEND_DTO))
            .thenThrow(IllegalStateException("database unavailable"))
        val service = AdvertisementContractService(userReader, appender, notifier, mailer)

        // when
        assertThrows(IllegalStateException::class.java) { service.createContract(null, COMMAND) }

        // then
        Mockito.verifyNoInteractions(notifier, mailer)
    }

    @Test
    @DisplayName("Slack provider 예외 로그에서 수신 이메일과 webhook URL을 숨긴다")
    fun `Slack provider 예외 로그에 수신 이메일과 webhook URL을 남기지 않는다`() {
        // given
        val userReader = Mockito.mock(UserReader::class.java)
        val appender = Mockito.mock(AdvertisementContractAppender::class.java)
        val notifier = Mockito.mock(AdvertisementContractSlackNotifier::class.java)
        val mailer = Mockito.mock(AdvertisementContractMailSender::class.java)
        Mockito.`when`(appender.appendFromForm(EXPECTED_APPEND_DTO)).thenReturn(CONTRACT_ID)
        Mockito.doThrow(
            RestClientException(
                "Slack request to $SENSITIVE_WEBHOOK included $SENSITIVE_EMAIL",
                RuntimeException("response body: $SENSITIVE_BODY"),
            ),
        ).`when`(notifier).notify(EXPECTED_NOTIFICATION)
        val logger = LoggerFactory.getLogger(AdvertisementContractService::class.java) as Logger
        val logAppender = ListAppender<ILoggingEvent>().apply {
            context = logger.loggerContext
            start()
        }
        logger.addAppender(logAppender)
        val service = AdvertisementContractService(userReader, appender, notifier, mailer)

        try {
            // when
            assertEquals(CONTRACT_ID, service.createContract(null, COMMAND))
        } finally {
            logger.detachAppender(logAppender)
            logAppender.stop()
        }

        // then
        val event = logAppender.list.single()
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
        Mockito.verify(mailer).sendConfirmation(EXPECTED_NOTIFICATION)
    }

    private companion object {
        const val USER_ID = 42L
        const val CONTRACT_ID = 1001L
        const val SENSITIVE_EMAIL = "private-recipient@example.com"
        const val SENSITIVE_WEBHOOK = "https://hooks.slack.com/services/fake-secret"
        const val SENSITIVE_BODY = "raw-provider-response"
        val COMMAND = CreateAdvertisementContractCommand(
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentUrl = "https://example.com/recruit/backend",
            recruitmentDeadline = LocalDate.of(2026, 12, 31),
            purpose = AdvertisementContractPurpose.FIT_APPLICANTS,
            categories = setOf(AdvertisementProductCategory.BANNER),
            scheduleRequest = "12월 첫째 주에 진행을 희망합니다.",
            advertisementRequest = "채용 안내 문구 검토를 요청합니다.",
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
        )
        val EXPECTED_APPEND_DTO = AdvertisementContractFormAppendDto(
            userId = null,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentUrl = "https://example.com/recruit/backend",
            recruitmentDeadline = LocalDate.of(2026, 12, 31),
            purpose = AdvertisementContractPurpose.FIT_APPLICANTS,
            categories = setOf(AdvertisementProductCategory.BANNER),
            scheduleRequest = "12월 첫째 주에 진행을 희망합니다.",
            advertisementRequest = "채용 안내 문구 검토를 요청합니다.",
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
        )
        val EXPECTED_NOTIFICATION = AdvertisementContractNotification(
            contractId = CONTRACT_ID,
            promotionTarget = "채용 공고",
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentDeadline = "2026-12-31",
            purpose = "적합한 지원자 확보",
            categories = listOf("배너"),
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
        )
    }
}
