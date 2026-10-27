package com.ogonggo.adminapi.advertisementinquiry.presentation

import com.ogonggo.adminapi.advertisementinquiry.business.AdminAdvertisementInquiryPageResult
import com.ogonggo.adminapi.advertisementinquiry.business.AdminAdvertisementInquiryService
import com.ogonggo.adminapi.advertisementinquiry.business.AdminAdvertisementInquirySummary
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryPromotionChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquirySearchCondition
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminAdvertisementInquiryController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdvertisementInquiryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdvertisementInquiryService: AdminAdvertisementInquiryService

    @Test
    fun `관리자 토큰이 없으면 광고 문의 목록과 오늘 건수를 볼 수 없다`() {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized)
        mockMvc.perform(get("$PATH/today-count")).andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminAdvertisementInquiryService)
    }

    @Test
    fun `필터 없이 부르면 조건 없이 1부터 시작하는 20건 페이지로 주고 문의의 모든 값을 싣는다`() {
        // given
        Mockito.`when`(adminAdvertisementInquiryService.getAdvertisementInquiries(AdvertisementInquirySearchCondition(), 0, 20))
            .thenReturn(AdminAdvertisementInquiryPageResult(listOf(SUMMARY), page = 0, size = 20, totalElements = 1, totalPages = 1))

        // when & then
        mockMvc.perform(get(PATH).with(admin()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(5))
            .andExpect(jsonPath("$.data.items[0].inquiryType").value("FREE_PROMOTION"))
            .andExpect(jsonPath("$.data.items[0].campaignCode").value("launch_free_promo"))
            .andExpect(jsonPath("$.data.items[0].status").value("RECEIVED"))
            .andExpect(jsonPath("$.data.items[0].companyName").value("렛츠커리어"))
            .andExpect(jsonPath("$.data.items[0].managerTitle").value("매니저"))
            .andExpect(jsonPath("$.data.items[0].promotionChannel").value("OPEN_CHAT_SALES"))
            .andExpect(jsonPath("$.data.items[0].marketingConsent").value(true))
            .andExpect(jsonPath("$.data.items[0].userId").value(3))
            .andExpect(jsonPath("$.data.items[0].memberType").value("COMPANY"))
            .andExpect(jsonPath("$.data.items[0].inflowChannel").value("COLDMAIL"))
            .andExpect(jsonPath("$.data.items[0].utmContent").value("templateA"))
            .andExpect(jsonPath("$.data.items[0].entryHost").value("biz.ogonggo.co.kr"))
            .andExpect(jsonPath("$.data.items[0].memo").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].registeredAt").value("2026-10-27T10:00:00"))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
            .andExpect(jsonPath("$.data.pageInfo.totalPages").value(1))
    }

    @Test
    fun `필터를 조합해 넘기고 빈 값은 보내지 않은 것으로 본다`() {
        // given
        val condition = AdvertisementInquirySearchCondition(
            inquiryType = AdvertisementInquiryType.FREE_PROMOTION,
            campaignCode = "launch_free_promo",
            status = AdvertisementInquiryStatus.CONTACTED,
            memberType = AdvertisementInquiryMemberType.GUEST,
            inflowChannel = AdvertisementInquiryInflowChannel.DIRECT,
            createdFrom = LocalDate.of(2026, 10, 27),
            createdTo = LocalDate.of(2026, 10, 27),
        )
        Mockito.`when`(adminAdvertisementInquiryService.getAdvertisementInquiries(condition, 1, 10))
            .thenReturn(AdminAdvertisementInquiryPageResult(emptyList(), page = 1, size = 10, totalElements = 12, totalPages = 2))
        Mockito.`when`(adminAdvertisementInquiryService.getAdvertisementInquiries(AdvertisementInquirySearchCondition(), 0, 20))
            .thenReturn(AdminAdvertisementInquiryPageResult(emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0))

        // when & then
        mockMvc.perform(
            get(PATH).with(admin())
                .param("page", "2")
                .param("size", "10")
                .param("inquiryType", "FREE_PROMOTION")
                .param("campaignCode", "launch_free_promo")
                .param("status", "CONTACTED")
                .param("memberType", "GUEST")
                .param("inflowChannel", "DIRECT")
                .param("createdFrom", "2026-10-27")
                .param("createdTo", "2026-10-27"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(2))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(12))
            .andExpect(jsonPath("$.data.pageInfo.totalPages").value(2))

        mockMvc.perform(
            get(PATH).with(admin())
                .param("inquiryType", "")
                .param("campaignCode", " ")
                .param("status", "")
                .param("memberType", "")
                .param("inflowChannel", ""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0))
    }

    @Test
    fun `정의되지 않은 필터 값과 거꾸로 된 기간, 페이지 범위는 400으로 응답한다`() {
        listOf(
            listOf("memberType" to "VIP") to null,
            listOf("inflowChannel" to "TWITTER") to null,
            listOf("createdFrom" to "2026-10-28", "createdTo" to "2026-10-27") to "createdFrom",
            // 페이지 범위는 메서드 검증이라 메시지 앞머리가 다르다. 400만 확인한다.
            listOf("size" to "101") to null,
        ).forEach { (params, field) ->
            val request = get(PATH).with(admin())
            params.forEach { (name, value) -> request.param(name, value) }
            val result = mockMvc.perform(request)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            if (field != null) {
                result.andExpect(jsonPath("$.message").value(startsWith("[$field]")))
            }
        }

        Mockito.verifyNoInteractions(adminAdvertisementInquiryService)
    }

    @Test
    fun `오늘 건수는 건수 하나만 준다`() {
        Mockito.`when`(adminAdvertisementInquiryService.getTodayCount()).thenReturn(3L)

        mockMvc.perform(get("$PATH/today-count").with(admin()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.count").value(3))
            .andExpect(jsonPath("$.data.length()").value(1))
    }

    private fun admin() = user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY))

    private companion object {
        const val PATH = "/api/v1/admin/advertisement-inquiries"

        val SUMMARY = AdminAdvertisementInquirySummary(
            id = 5L,
            inquiryType = AdvertisementInquiryType.FREE_PROMOTION,
            campaignCode = "launch_free_promo",
            status = AdvertisementInquiryStatus.RECEIVED,
            companyName = "렛츠커리어",
            managerName = "김담당",
            managerTitle = "매니저",
            email = "manager@ogonggo.co.kr",
            phoneNumber = "010-1234-5678",
            promotionChannel = AdvertisementInquiryPromotionChannel.OPEN_CHAT_SALES,
            hiringJobName = "영업 인턴",
            jobPostingUrl = "https://example.com/jobs/1",
            promotionAnswer = "사람인에 올리고 있습니다.",
            marketingConsent = true,
            userId = 3L,
            memberType = AdvertisementInquiryMemberType.COMPANY,
            inflowChannel = AdvertisementInquiryInflowChannel.COLDMAIL,
            utmSource = "coldmail",
            utmMedium = "email",
            utmCampaign = "free_promo",
            utmContent = "templateA",
            referrer = null,
            entryHost = "biz.ogonggo.co.kr",
            landingPath = "/biz?utm_source=coldmail&entry=biz",
            memo = null,
            registeredAt = LocalDateTime.of(2026, 10, 27, 10, 0),
        )
    }
}
