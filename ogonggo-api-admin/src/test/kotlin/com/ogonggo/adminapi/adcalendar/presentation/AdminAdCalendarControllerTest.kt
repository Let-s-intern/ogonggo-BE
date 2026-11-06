package com.ogonggo.adminapi.adcalendar.presentation

import com.ogonggo.adminapi.adcalendar.business.AdminAdCalendarAdvertiserResult
import com.ogonggo.adminapi.adcalendar.business.AdminAdCalendarContractResult
import com.ogonggo.adminapi.adcalendar.business.AdminAdCalendarResult
import com.ogonggo.adminapi.adcalendar.business.AdminAdCalendarService
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.calendartodo.business.AdminCalendarDateRange
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminAdCalendarController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdCalendarControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdCalendarService: AdminAdCalendarService

    @Test
    fun `광고 일정은 계약·배너·광고 공고·채널 광고 네 배열을 싣고 계약에 광고주 요약을 붙인다`() {
        // given
        val start = LocalDateTime.of(2026, 11, 1, 0, 0)
        Mockito.`when`(adminAdCalendarService.getAdCalendar(AdminCalendarDateRange(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 12, 7))))
            .thenReturn(
                AdminAdCalendarResult(
                    contracts = listOf(
                        AdminAdCalendarContractResult(
                            id = 1,
                            name = "11월 공고 상세 패키지",
                            startAt = start,
                            endAt = start.plusMonths(1),
                            advertiserId = 1,
                            advertiser = AdminAdCalendarAdvertiserResult(1, "그로우랩", AdvertiserType.NON_MEMBER),
                            amount = 1_500_000,
                            paid = true,
                            products = listOf(AdContractProduct.BANNER_JOB_DETAIL_SIDE, AdContractProduct.AD_POSTING_JOB_DETAIL_SIDE),
                        ),
                    ),
                    banners = emptyList(),
                    adPostings = emptyList(),
                    channelAds = emptyList(),
                ),
            )

        // when & then
        mockMvc.perform(admin(get("/api/v1/admin/ad-calendar").param("from", "2026-10-26").param("to", "2026-12-07")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.contracts[0].startAt").value("2026-11-01T00:00:00"))
            .andExpect(jsonPath("$.data.contracts[0].endAt").value("2026-12-01T00:00:00"))
            .andExpect(jsonPath("$.data.contracts[0].advertiser.name").value("그로우랩"))
            .andExpect(jsonPath("$.data.contracts[0].advertiser.type").value("NON_MEMBER"))
            .andExpect(jsonPath("$.data.contracts[0].products[1]").value("AD_POSTING_JOB_DETAIL_SIDE"))
            .andExpect(jsonPath("$.data.banners").isArray)
            .andExpect(jsonPath("$.data.adPostings").isArray)
            .andExpect(jsonPath("$.data.channelAds").isArray)
    }

    @Test
    fun `앞뒤가 바뀐 기간은 화면 문구로 400을 준다`() {
        mockMvc.perform(admin(get("/api/v1/admin/ad-calendar").param("from", "2026-12-07").param("to", "2026-10-26")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("from, to: YYYY-MM-DD(KST) 로, from 이 to 보다 앞서게 보내 주세요."))
    }

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder = request.with(
        authentication(UsernamePasswordAuthenticationToken(9L, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))),
    )
}
