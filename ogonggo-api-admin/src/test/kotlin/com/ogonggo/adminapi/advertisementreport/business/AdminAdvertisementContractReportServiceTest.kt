package com.ogonggo.adminapi.advertisementreport.business

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementevent.implement.AdvertisementContractReportProvider
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportMetricsDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportSummaryDto
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AdminAdvertisementContractReportServiceTest {

    @Test
    @DisplayName("관리자 JSON·CSV service는 계약 노출 allowlist와 공통 report provider를 사용한다")
    fun `관리자 리포트는 동일 provider와 계약 노출 allowlist를 사용한다`() {
        val contractReader = Mockito.mock(AdvertisementContractReader::class.java)
        val exposureReader = Mockito.mock(AdvertisementExposureReader::class.java)
        val provider = Mockito.mock(AdvertisementContractReportProvider::class.java)
        val exposureIds = listOf(31L, 32L)
        val report = AdvertisementContractReportDto(
            contractId = CONTRACT_ID,
            from = FROM,
            to = TODAY,
            summary = AdvertisementReportSummaryDto(ZERO, ZERO),
            daily = emptyList(),
            positions = emptyList(),
            jobRoleDistribution = emptyList(),
        )
        Mockito.`when`(exposureReader.readIdsByContractId(CONTRACT_ID)).thenReturn(exposureIds)
        Mockito.`when`(provider.read(CONTRACT_ID, FROM, TODAY, exposureIds)).thenReturn(report)
        val service = AdminAdvertisementContractReportService(contractReader, exposureReader, provider, CLOCK)

        val result = service.getReport(CONTRACT_ID, FROM, TODAY)

        assertEquals(report, result)
        Mockito.verify(provider).read(CONTRACT_ID, FROM, TODAY, exposureIds)
    }

    private companion object {
        const val CONTRACT_ID = 420L
        val TODAY = LocalDate.of(2026, 11, 19)
        val FROM = TODAY.minusDays(1)
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-11-19T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        val ZERO = AdvertisementReportMetricsDto(total = 0, uniqueUsers = 0, perUser = 0.0)
    }
}
