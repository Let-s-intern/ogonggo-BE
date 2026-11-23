package com.ogonggo.adminapi.advertisementcontract.presentation

import com.ogonggo.adminapi.advertisementcontract.business.AdminAdvertisementReportService
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportAudienceDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportCsvRowDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetMatchDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDailyDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportExposureDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportProductDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportProductSumDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTotalDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTotalSumDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSummaryDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@WebMvcTest(controllers = [AdminAdvertisementReportController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdvertisementReportControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdvertisementReportService: AdminAdvertisementReportService

    @Test
    fun `관리자는 어느 계약이든 리포트와 CSV를 받는다`() {
        // given
        Mockito.`when`(adminAdvertisementReportService.getReport(1L, 7L, null, null)).thenReturn(REPORT)

        // when
        mockMvc.perform(admin(get(PATH)))
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.product.category").value("BANNER"))
            .andExpect(jsonPath("$.data.summary.scrollFunnel").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.daily[0].avgScrollPercent").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.audience.reach").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.audience.targetMatch.members").value(0))
        val csv = mockMvc.perform(admin(get("$PATH.csv"))).andExpect(status().isOk).andReturn().response
        val lines = String(csv.contentAsByteArray, StandardCharsets.UTF_8).removePrefix("﻿").trimEnd('\n').split("\n")
        assertEquals(1 + REPORT.csvRows.size, lines.size)
        assertEquals("2026-11-01,12,11월 홈 상단,100,2,1,1,0,0,0,,", lines[1])
    }

    @Test
    fun `없는 계약은 404, 리포트가 없는 상품은 409, 관리자 토큰이 없으면 401이다`() {
        // given
        Mockito.`when`(adminAdvertisementReportService.getReport(1L, 8L, null, null))
            .thenThrow(EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND))
        Mockito.`when`(adminAdvertisementReportService.getReport(1L, 9L, null, null))
            .thenThrow(ConflictException(AdvertisementContractErrorCode.REPORT_NOT_AVAILABLE))

        // then
        mockMvc.perform(admin(get("/api/v1/admin/advertisement-contracts/1/products/8/report")))
            .andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND"))
        mockMvc.perform(admin(get("/api/v1/admin/advertisement-contracts/1/products/9/report")))
            .andExpect(status().isConflict).andExpect(jsonPath("$.code").value("REPORT_NOT_AVAILABLE"))
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized)
    }

    @Test
    fun `관리자는 계약 전체 합계를 받는다`() {
        // given
        Mockito.`when`(adminAdvertisementReportService.getTotal(1L)).thenReturn(
            AdvertisementContractReportTotalDto(
                total = AdvertisementContractReportTotalSumDto(100, 80, 2, 0.02, 1, 0),
                products = listOf(
                    AdvertisementContractReportProductSumDto(
                        7L, AdvertisementProductCategory.BANNER, AdvertisementProductPosition.JOB_HOME_TOP_BANNER, "홈 상단 배너", FROM, FROM, 100, 80, 2, 1, 0,
                    ),
                ),
            ),
        )

        // then
        mockMvc.perform(admin(get("/api/v1/admin/advertisement-contracts/1/report-summary")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.total.impressions").value(100))
            .andExpect(jsonPath("$.data.products[0].position").value("JOB_HOME_TOP_BANNER"))
    }

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val PATH = "/api/v1/admin/advertisement-contracts/1/products/7/report"
        val FROM: LocalDate = LocalDate.of(2026, 11, 1)

        val REPORT = AdvertisementContractReportDto(
            product = AdvertisementContractReportProductDto(
                1L, 7L, 3L, AdvertisementProductCategory.BANNER, AdvertisementProductPosition.JOB_HOME_TOP_BANNER, "홈 상단 배너", FROM, LocalDate.of(2026, 11, 30),
            ),
            from = FROM,
            to = FROM,
            collectedSince = FROM,
            uniqueCountsApproximate = false,
            summary = AdvertisementContractReportSummaryDto(100, 80, 2, 0.02, 1, 1, 0, 0, 0, 0, 0, 0.0, null, null, null),
            daily = listOf(AdvertisementContractReportDailyDto(FROM, 100, 80, 2, 1, 1, 0, 0, 0, null, null)),
            exposures = listOf(AdvertisementContractReportExposureDto(12L, "11월 홈 상단", 100, 2, 0.02)),
            targets = emptyList(),
            byJobField = emptyList(),
            audience = AdvertisementContractReportAudienceDto(
                reach = null,
                avgFrequency = null,
                adClickVisitors = null,
                targetJobFields = emptyList(),
                targetMatch = AdvertisementContractReportTargetMatchDto(matchedMembers = 0, members = 0, rate = null),
                byGrade = emptyList(),
            ),
            csvRows = listOf(AdvertisementContractReportCsvRowDto(FROM, 12L, "11월 홈 상단", 100, 2, 1, 1, 0, 0, 0, null, null)),
        )
    }
}
