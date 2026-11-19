package com.ogonggo.userapi.advertisementevent.business

import com.ogonggo.core.advertisementevent.error.AdvertisementEventErrorCode
import com.ogonggo.core.advertisementevent.implement.AdvertisementContractReportProvider
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportMetricsDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportSummaryDto
import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.userapi.advertisementevent.implement.AdvertisementReportAccess
import com.ogonggo.userapi.advertisementevent.implement.AdvertisementReportAccessReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AdvertisementContractReportServiceTest {

    @Test
    @DisplayName("기업 계약 소유자는 서버 조회한 노출 ID만 provider에 전달한다")
    fun `기업 계약 소유자는 서버 조회한 노출 ID만 provider에 전달한다`() {
        // given
        val accessReader = Mockito.mock(AdvertisementReportAccessReader::class.java)
        val provider = Mockito.mock(AdvertisementContractReportProvider::class.java)
        val access = AdvertisementReportAccess(OWNER_ID, UserRole.COMPANY, listOf(11, 12))
        Mockito.`when`(accessReader.read(OWNER_ID, CONTRACT_ID)).thenReturn(access)
        Mockito.`when`(provider.read(CONTRACT_ID, FROM, TO, access.exposureIds)).thenReturn(REPORT)
        val service = service(accessReader, provider)

        // when
        val result = service.getForCompanyMember(OWNER_ID, CONTRACT_ID, FROM, TO)

        // then
        assertEquals(REPORT, result)
        Mockito.verify(provider).read(CONTRACT_ID, FROM, TO, listOf(11, 12))
    }

    @Test
    @DisplayName("다른 회원의 계약은 report provider를 호출하지 않는다")
    fun `다른 회원 계약은 report provider를 호출하지 않는다`() {
        // given
        val accessReader = Mockito.mock(AdvertisementReportAccessReader::class.java)
        val provider = Mockito.mock(AdvertisementContractReportProvider::class.java)
        Mockito.`when`(accessReader.read(REQUESTER_ID, CONTRACT_ID))
            .thenReturn(AdvertisementReportAccess(OWNER_ID, UserRole.COMPANY, listOf(11)))
        val service = service(accessReader, provider)

        // when
        val exception = assertThrows(BusinessException::class.java) {
            service.getForCompanyMember(REQUESTER_ID, CONTRACT_ID, FROM, TO)
        }

        // then
        assertEquals(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_FORBIDDEN, exception.errorCode)
        Mockito.verifyNoInteractions(provider)
    }

    @Test
    @DisplayName("기업 역할이 아니면 report provider를 호출하지 않는다")
    fun `기업 역할이 아니면 report provider를 호출하지 않는다`() {
        // given
        val accessReader = Mockito.mock(AdvertisementReportAccessReader::class.java)
        val provider = Mockito.mock(AdvertisementContractReportProvider::class.java)
        Mockito.`when`(accessReader.read(OWNER_ID, CONTRACT_ID))
            .thenReturn(AdvertisementReportAccess(OWNER_ID, UserRole.USER, listOf(11)))
        val service = service(accessReader, provider)

        // when
        val exception = assertThrows(BusinessException::class.java) {
            service.getForCompanyMember(OWNER_ID, CONTRACT_ID, FROM, TO)
        }

        // then
        assertEquals(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_FORBIDDEN, exception.errorCode)
        Mockito.verifyNoInteractions(provider)
    }

    @Test
    @DisplayName("잘못된 기간은 report provider를 호출하지 않는다")
    fun `잘못된 기간은 report provider를 호출하지 않는다`() {
        // given
        val accessReader = Mockito.mock(AdvertisementReportAccessReader::class.java)
        val provider = Mockito.mock(AdvertisementContractReportProvider::class.java)
        Mockito.`when`(accessReader.read(OWNER_ID, CONTRACT_ID))
            .thenReturn(AdvertisementReportAccess(OWNER_ID, UserRole.COMPANY, listOf(11)))
        val service = service(accessReader, provider)

        // when
        val exception = assertThrows(BusinessException::class.java) {
            service.getForCompanyMember(OWNER_ID, CONTRACT_ID, TO, FROM)
        }

        // then
        assertEquals(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_PERIOD_INVALID, exception.errorCode)
        Mockito.verifyNoInteractions(provider)
    }

    private fun service(
        accessReader: AdvertisementReportAccessReader,
        provider: AdvertisementContractReportProvider,
    ) = AdvertisementContractReportService(accessReader, provider, FIXED_CLOCK, NO_OP_TRANSACTION_MANAGER)

    private companion object {
        const val REQUESTER_ID = 9L
        const val OWNER_ID = 10L
        const val CONTRACT_ID = 42L
        val FROM: LocalDate = LocalDate.of(2026, 11, 1)
        val TO: LocalDate = LocalDate.of(2026, 11, 19)
        val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-11-19T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        val ZERO = AdvertisementReportMetricsDto(total = 0, uniqueUsers = 0, perUser = 0.0)
        val REPORT = AdvertisementContractReportDto(
            contractId = CONTRACT_ID,
            from = FROM,
            to = TO,
            summary = AdvertisementReportSummaryDto(ZERO, ZERO),
            daily = emptyList(),
            positions = emptyList(),
            jobRoleDistribution = emptyList(),
        )
        val NO_OP_TRANSACTION_MANAGER = object : PlatformTransactionManager {
            override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()
            override fun commit(status: TransactionStatus) = Unit
            override fun rollback(status: TransactionStatus) = Unit
        }
    }
}
