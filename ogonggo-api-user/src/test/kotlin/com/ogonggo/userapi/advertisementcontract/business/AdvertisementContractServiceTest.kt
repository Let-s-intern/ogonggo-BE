package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractBudgetRange
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractFormAppendDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.advertisementcontract.implement.AdvertisementContractNotificationAppender
import com.ogonggo.userapi.advertisementcontract.implement.dto.AdvertisementContractNotificationDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate

class AdvertisementContractServiceTest {

    @Test
    @DisplayName("회원 행이 있으면 연결해 신청을 저장하고 알림을 적재한다")
    fun `회원 행이 있으면 연결해 신청을 저장하고 알림을 적재한다`() {
        // given
        val userReader = Mockito.mock(UserReader::class.java)
        val contractAppender = Mockito.mock(AdvertisementContractAppender::class.java)
        val notificationAppender = Mockito.mock(AdvertisementContractNotificationAppender::class.java)
        Mockito.`when`(userReader.readRoles(setOf(USER_ID))).thenReturn(mapOf(USER_ID to UserRole.USER))
        Mockito.`when`(contractAppender.appendFromForm(EXPECTED_APPEND_DTO.copy(userId = USER_ID))).thenReturn(CONTRACT_ID)
        val service = AdvertisementContractService(userReader, contractAppender, notificationAppender)

        // when
        val result = service.createContract(USER_ID, COMMAND)

        // then
        assertEquals(CONTRACT_ID, result)
        Mockito.verify(contractAppender).appendFromForm(EXPECTED_APPEND_DTO.copy(userId = USER_ID))
        Mockito.verify(notificationAppender).append(AdvertisementContractNotificationDto.from(CONTRACT_ID, COMMAND))
    }

    @Test
    @DisplayName("회원 행이 없는 토큰은 비회원 신청으로 저장한다")
    fun `회원 행이 없는 토큰은 비회원 신청으로 저장한다`() {
        // given
        val userReader = Mockito.mock(UserReader::class.java)
        val contractAppender = Mockito.mock(AdvertisementContractAppender::class.java)
        val notificationAppender = Mockito.mock(AdvertisementContractNotificationAppender::class.java)
        Mockito.`when`(userReader.readRoles(setOf(USER_ID))).thenReturn(emptyMap())
        Mockito.`when`(contractAppender.appendFromForm(EXPECTED_APPEND_DTO)).thenReturn(CONTRACT_ID)
        val service = AdvertisementContractService(userReader, contractAppender, notificationAppender)

        // when
        val result = service.createContract(USER_ID, COMMAND)

        // then
        assertEquals(CONTRACT_ID, result)
        assertEquals(null, EXPECTED_APPEND_DTO.userId)
        Mockito.verify(notificationAppender).append(AdvertisementContractNotificationDto.from(CONTRACT_ID, COMMAND))
    }

    @Test
    @DisplayName("알림 적재 실패가 저장된 신청 접수 응답을 바꾸지 않는다")
    fun `알림 적재 실패가 저장된 신청 접수 응답을 바꾸지 않는다`() {
        // given
        val contractAppender = Mockito.mock(AdvertisementContractAppender::class.java)
        val notificationAppender = Mockito.mock(AdvertisementContractNotificationAppender::class.java)
        Mockito.`when`(contractAppender.appendFromForm(EXPECTED_APPEND_DTO)).thenReturn(CONTRACT_ID)
        Mockito.doThrow(IllegalStateException("notification storage unavailable"))
            .`when`(notificationAppender).append(AdvertisementContractNotificationDto.from(CONTRACT_ID, COMMAND))
        val service = AdvertisementContractService(
            Mockito.mock(UserReader::class.java),
            contractAppender,
            notificationAppender,
        )

        // when
        val result = service.createContract(null, COMMAND)

        // then
        assertEquals(CONTRACT_ID, result)
    }

    @Test
    @DisplayName("신청 저장이 실패하면 알림 적재 없이 오류를 전달한다")
    fun `신청 저장이 실패하면 알림 적재 없이 오류를 전달한다`() {
        // given
        val contractAppender = Mockito.mock(AdvertisementContractAppender::class.java)
        val notificationAppender = Mockito.mock(AdvertisementContractNotificationAppender::class.java)
        Mockito.`when`(contractAppender.appendFromForm(EXPECTED_APPEND_DTO))
            .thenThrow(IllegalStateException("database unavailable"))
        val service = AdvertisementContractService(
            Mockito.mock(UserReader::class.java),
            contractAppender,
            notificationAppender,
        )

        // when / then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
            service.createContract(null, COMMAND)
        }
        Mockito.verifyNoInteractions(notificationAppender)
    }

    private companion object {
        const val USER_ID = 42L
        const val CONTRACT_ID = 1001L
        val COMMAND = CreateAdvertisementContractCommand(
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentUrl = "https://example.com/recruit/backend",
            recruitmentDeadline = LocalDate.of(2026, 12, 31),
            purpose = AdvertisementContractPurpose.FIT_APPLICANTS,
            budgetRange = AdvertisementContractBudgetRange.UNDECIDED,
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
            budgetRange = AdvertisementContractBudgetRange.UNDECIDED,
            categories = setOf(AdvertisementProductCategory.BANNER),
            scheduleRequest = "12월 첫째 주에 진행을 희망합니다.",
            advertisementRequest = "채용 안내 문구 검토를 요청합니다.",
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
        )
    }
}
