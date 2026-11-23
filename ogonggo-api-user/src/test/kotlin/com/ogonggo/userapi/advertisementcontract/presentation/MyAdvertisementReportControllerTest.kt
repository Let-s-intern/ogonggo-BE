package com.ogonggo.userapi.advertisementcontract.presentation

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractActiveDailyDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractActiveReportDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportAudienceDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportCsvRowDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportGradeDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetMatchDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementReportInsightDto
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportPostingDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportProductSumDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTotalDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTotalSumDto
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDailyDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportExposureDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportJobFieldDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportProductDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportScrollStepDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSourceDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSummaryDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetTotalDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.userapi.advertisementcontract.business.MyAdvertisementReportService
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@WebMvcTest(controllers = [MyAdvertisementReportController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class MyAdvertisementReportControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var myAdvertisementReportService: MyAdvertisementReportService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `리포트는 기간을 넘겨 상품 몫 요약과 날짜별 추이와 대상 공고를 준다`() {
        // given
        Mockito.`when`(myAdvertisementReportService.getReport(USER_ID, CONTRACT_ID, PRODUCT_ID, FROM, TO)).thenReturn(REPORT)

        // when
        mockMvc.perform(get(REPORT_PATH).param("from", "2026-11-01").param("to", "2026-11-02").with(authenticatedUser()))
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.product.contractProductId").value(PRODUCT_ID))
            .andExpect(jsonPath("$.data.product.price").value(300_000))
            .andExpect(jsonPath("$.data.from").value("2026-11-01"))
            .andExpect(jsonPath("$.data.summary.adClickRate").value(0.02))
            .andExpect(jsonPath("$.data.summary.scrollFunnel[2].threshold").value(50))
            .andExpect(jsonPath("$.data.summary.scrollFunnel[2].rate").value(0.7))
            .andExpect(jsonPath("$.data.daily.length()").value(2))
            .andExpect(jsonPath("$.data.exposures[0].name").value("백엔드 개발자 · 뱅크"))
            .andExpect(jsonPath("$.data.targets[0].bySource[0].source").value("AD_CARD"))
            .andExpect(jsonPath("$.data.uniqueCountsApproximate").value(false))
            .andExpect(jsonPath("$.data.summary.uniqueImpressions").value(150))
            .andExpect(jsonPath("$.data.daily[0].uniqueImpressions").value(80))
            .andExpect(jsonPath("$.data.byJobField[0].jobField").value("IT_DEVELOPMENT"))
            .andExpect(jsonPath("$.data.byJobField[1].jobField").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.byJobField[1].label").value("직무 미설정"))
            .andExpect(jsonPath("$.data.audience.reach").value(120))
            .andExpect(jsonPath("$.data.audience.avgFrequency").value(1.7))
            .andExpect(jsonPath("$.data.audience.targetJobFields[0]").value("IT_DEVELOPMENT"))
            .andExpect(jsonPath("$.data.audience.targetMatch.rate").value(0.6667))
            .andExpect(jsonPath("$.data.audience.byGrade[0].grade").value("FOURTH"))
            .andExpect(jsonPath("$.data.audience.byGrade[1].label").value("학년 미설정"))
            .andExpect(jsonPath("$.data.targets[0].total.saves").value(3))
            .andExpect(jsonPath("$.data.targets[0].total.shares").value(1))
            .andExpect(jsonPath("$.data.insights[0].title").value("마감 전 재노출을 제안해요"))
            .andExpect(jsonPath("$.data.insights[0].published").doesNotExist())
    }

    @Test
    fun `CSV는 BOM과 머리줄 뒤에 날짜별·소재별 한 줄씩 준다`() {
        // given
        Mockito.`when`(myAdvertisementReportService.getReport(USER_ID, CONTRACT_ID, PRODUCT_ID, null, null)).thenReturn(REPORT)

        // when
        val response = mockMvc.perform(get("$REPORT_PATH.csv").with(authenticatedUser()))
            // then
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"report-7-2026-11-01_2026-11-02.csv\""))
            .andReturn().response
        val body = String(response.contentAsByteArray, StandardCharsets.UTF_8)
        val lines = body.removePrefix("﻿").trimEnd('\n').split("\n")
        assertTrue(body.startsWith("﻿"))
        assertEquals(
            "date,exposureId,exposureName,impressions,adClicks,views,uniqueVisitors,memberViews,ctaClicks,ctaUniqueVisitors,avgScrollPercent,avgDurationSeconds",
            lines.first(),
        )
        assertEquals(1 + REPORT.csvRows.size, lines.size)
        assertEquals("2026-11-01,31,\"공고, 광고\",100,2,1,1,0,0,0,50.0,", lines[1])
    }

    @Test
    fun `기업 회원이 아니면 403, 남의 계약은 404, 리포트가 없는 상품은 409다`() {
        // given
        Mockito.`when`(myAdvertisementReportService.getReport(USER_ID, 2L, PRODUCT_ID, null, null))
            .thenThrow(ForbiddenException(UserErrorCode.COMPANY_ROLE_REQUIRED))
        Mockito.`when`(myAdvertisementReportService.getReport(USER_ID, 3L, PRODUCT_ID, null, null))
            .thenThrow(EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND))
        Mockito.`when`(myAdvertisementReportService.getReport(USER_ID, 4L, PRODUCT_ID, null, null))
            .thenThrow(ConflictException(AdvertisementContractErrorCode.REPORT_NOT_AVAILABLE))

        // then
        mockMvc.perform(get(pathOf(2L)).with(authenticatedUser()))
            .andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("COMPANY_ROLE_REQUIRED"))
        mockMvc.perform(get(pathOf(3L)).with(authenticatedUser()))
            .andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("ADVERTISEMENT_CONTRACT_NOT_FOUND"))
        mockMvc.perform(get(pathOf(4L)).with(authenticatedUser()))
            .andExpect(status().isConflict).andExpect(jsonPath("$.code").value("REPORT_NOT_AVAILABLE"))
        mockMvc.perform(get(pathOf(4L)))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `진행 중 광고 요약과 계약 합계를 준다`() {
        // given
        Mockito.`when`(myAdvertisementReportService.getActive(USER_ID)).thenReturn(
            listOf(
                AdvertisementContractActiveReportDto(
                    contractId = CONTRACT_ID, contractProductId = PRODUCT_ID, category = AdvertisementProductCategory.JOB_POSTING,
                    position = null, name = "목록 광고 공고", startDate = FROM, endDate = TO, daysLeft = 1, impressions = 200,
                    adClicks = 4, adClickRate = 0.02, views = 2, ctaClicks = 0, scroll50Rate = 0.7,
                    target = AdvertisementContractReportPostingDto(AdvertisementExposureTargetType.JOB, 15818L, "백엔드 개발자", "뱅크", null, null),
                    daily = listOf(AdvertisementContractActiveDailyDto(FROM, 100, 2, 1)),
                ),
            ),
        )
        Mockito.`when`(myAdvertisementReportService.getTotal(USER_ID, CONTRACT_ID)).thenReturn(
            AdvertisementContractReportTotalDto(
                total = AdvertisementContractReportTotalSumDto(200, 150, 4, 0.02, 2, 0),
                products = listOf(
                    AdvertisementContractReportProductSumDto(PRODUCT_ID, AdvertisementProductCategory.JOB_POSTING, null, "목록 광고 공고", FROM, TO, 200, 150, 4, 2, 0),
                ),
            ),
        )

        // then
        mockMvc.perform(get("/api/v1/users/me/advertisement-reports/active").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].contractProductId").value(PRODUCT_ID))
            .andExpect(jsonPath("$.data[0].scroll50Rate").value(0.7))
            .andExpect(jsonPath("$.data[0].target.title").value("백엔드 개발자"))
            .andExpect(jsonPath("$.data[0].target.companyName").doesNotExist())
            .andExpect(jsonPath("$.data[0].daily[0].views").value(1))
        mockMvc.perform(get("/api/v1/users/me/advertisement-contracts/$CONTRACT_ID/report-summary").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.total.uniqueImpressions").value(150))
            .andExpect(jsonPath("$.data.products[0].contractProductId").value(PRODUCT_ID))
        mockMvc.perform(get("/api/v1/users/me/advertisement-reports/active")).andExpect(status().isUnauthorized)
    }

    private fun pathOf(contractId: Long) = "/api/v1/users/me/advertisement-contracts/$contractId/products/$PRODUCT_ID/report"

    private fun authenticatedUser() = authentication(UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()))

    private companion object {
        const val USER_ID = 42L
        const val CONTRACT_ID = 1L
        const val PRODUCT_ID = 7L
        const val REPORT_PATH = "/api/v1/users/me/advertisement-contracts/1/products/7/report"
        val FROM: LocalDate = LocalDate.of(2026, 11, 1)
        val TO: LocalDate = LocalDate.of(2026, 11, 2)

        val REPORT = AdvertisementContractReportDto(
            product = AdvertisementContractReportProductDto(1L, PRODUCT_ID, 3L, AdvertisementProductCategory.JOB_POSTING, null, "목록 광고 공고", FROM, LocalDate.of(2026, 11, 30), 300_000),
            from = FROM,
            to = TO,
            collectedSince = FROM,
            uniqueCountsApproximate = false,
            summary = AdvertisementContractReportSummaryDto(
                impressions = 200, uniqueImpressions = 150, adClicks = 4, adClickRate = 0.02, views = 2, uniqueVisitors = 2, memberViews = 0,
                memberUniqueVisitors = 0, ctaClicks = 0, ctaUniqueVisitors = 0, ctaUniqueMembers = 0, ctaClickRate = 0.0,
                avgScrollPercent = 50.0,
                scrollFunnel = listOf(0, 25, 50, 75, 100).zip(listOf(1.0, 0.9, 0.7, 0.45, 0.2)).map { (t, r) ->
                    AdvertisementContractReportScrollStepDto(t, 2, r)
                },
                avgDurationSeconds = null,
            ),
            daily = listOf(FROM, TO).map { AdvertisementContractReportDailyDto(it, 100, 80, 2, 1, 1, 0, 0, 0, 50.0, 0.0) },
            exposures = listOf(AdvertisementContractReportExposureDto(31L, "백엔드 개발자 · 뱅크", 200, 4, 0.02)),
            targets = listOf(
                AdvertisementContractReportTargetDto(
                    targetType = AdvertisementContractPromotionTarget.JOB,
                    targetId = 15818L,
                    title = "백엔드 개발자",
                    total = AdvertisementContractReportTargetTotalDto(3, 1, 10, 8, 4, 2, 2, 40.0, 30.0),
                    bySource = ContentEventSource.entries.map { AdvertisementContractReportSourceDto(it, 1, 0) },
                ),
            ),
            byJobField = listOf(
                AdvertisementContractReportJobFieldDto(JobField.IT_DEVELOPMENT, "IT·개발", 2, 3, 1, 1),
                AdvertisementContractReportJobFieldDto(null, "직무 미설정", 1, 1, 0, 0),
            ),
            audience = AdvertisementContractReportAudienceDto(
                reach = 120,
                avgFrequency = 1.7,
                adClickVisitors = 4,
                targetJobFields = listOf(JobField.IT_DEVELOPMENT),
                targetMatch = AdvertisementContractReportTargetMatchDto(matchedMembers = 2, members = 3, rate = 0.6667),
                byGrade = listOf(
                    AdvertisementContractReportGradeDto(UserGrade.FOURTH, "4학년", 2),
                    AdvertisementContractReportGradeDto(null, "학년 미설정", 1),
                ),
            ),
            insights = listOf(AdvertisementReportInsightDto("마감 전 재노출을 제안해요", "마감 2주 전 한 번 더 노출해 보세요.", true)),
            csvRows = listOf(FROM, TO).map {
                AdvertisementContractReportCsvRowDto(it, 31L, "공고, 광고", 100, 2, 1, 1, 0, 0, 0, 50.0, null)
            },
        )
    }
}
