package com.ogonggo.adminapi.advertiser.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.advertiser.business.AdminAdvertiserContractTotals
import com.ogonggo.adminapi.advertiser.business.AdminAdvertiserCreateCommand
import com.ogonggo.adminapi.advertiser.business.AdminAdvertiserResult
import com.ogonggo.adminapi.advertiser.business.AdminAdvertiserService
import com.ogonggo.adminapi.advertiser.business.AdminAdvertiserUpdateCommand
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.advertiser.domain.AdvertiserSearchCondition
import com.ogonggo.core.advertiser.domain.AdvertiserSource
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertiser.implement.dto.AdvertiserAdCountDto
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AdminAdvertiserController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminAdvertiserControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminAdvertiserService: AdminAdvertiserService

    @Test
    fun `관리자 토큰이 없으면 광고주 목록을 볼 수 없다`() {
        mockMvc.perform(get("/api/v1/admin/advertisers"))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminAdvertiserService)
    }

    @Test
    fun `목록은 종류와 검색어를 조건으로 넘기고 광고 수와 계약 합계를 싣되 값이 없는 선택 칸은 뺀다`() {
        // given
        Mockito.`when`(
            adminAdvertiserService.getAdvertisers(AdvertiserSearchCondition(type = AdvertiserType.NON_MEMBER, keyword = "그로우")),
        ).thenReturn(listOf(result()))

        // when & then
        mockMvc.perform(admin(get("/api/v1/admin/advertisers").param("type", "NON_MEMBER").param("keyword", " 그로우 ")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(ADVERTISER_ID))
            .andExpect(jsonPath("$.data[0].type").value("NON_MEMBER"))
            .andExpect(jsonPath("$.data[0].source").value("MANUAL"))
            .andExpect(jsonPath("$.data[0].contactName").value("정하늘"))
            .andExpect(jsonPath("$.data[0].companyMemberUserId").doesNotExist())
            .andExpect(jsonPath("$.data[0].adInquiryId").doesNotExist())
            .andExpect(jsonPath("$.data[0].contactEmail").doesNotExist())
            .andExpect(jsonPath("$.data[0].bannerCount").value(0))
            .andExpect(jsonPath("$.data[0].activeAdPostingCount").value(0))
            .andExpect(jsonPath("$.data[0].latestEndAt").isEmpty)
            .andExpect(jsonPath("$.data[0].contractCount").value(2))
            .andExpect(jsonPath("$.data[0].activeContractCount").value(1))
            .andExpect(jsonPath("$.data[0].contractTotalAmount").value(3_000_000))
            .andExpect(jsonPath("$.data[0].contractVatMixed").value(true))
    }

    @Test
    fun `모르는 종류는 화면 문구로 400이다`() {
        mockMvc.perform(admin(get("/api/v1/admin/advertisers").param("type", "PARTNER")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("type: MEMBER, NON_MEMBER 중 하나를 보내 주세요."))

        Mockito.verifyNoInteractions(adminAdvertiserService)
    }

    @Test
    fun `비회원 광고주를 추가하면 201과 추가한 광고주를 준다`() {
        // given
        val command = AdminAdvertiserCreateCommand(name = "그로우랩", contactName = "정하늘")
        Mockito.`when`(adminAdvertiserService.createAdvertiser(command)).thenReturn(ADVERTISER_ID)
        Mockito.`when`(adminAdvertiserService.getAdvertiser(ADVERTISER_ID)).thenReturn(result())

        // when & then
        mockMvc.perform(
            admin(post("/api/v1/admin/advertisers"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("name" to "그로우랩", "contactName" to "정하늘"))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.name").value("그로우랩"))
    }

    @Test
    fun `서비스가 거른 칸은 문구를 바꾸지 않고 BAD_REQUEST로 준다`() {
        // given
        val command = AdminAdvertiserUpdateCommand(name = "새싹")
        Mockito.`when`(adminAdvertiserService.updateAdvertiser(ADVERTISER_ID, command))
            .thenThrow(AdminConsoleBadRequestException.field("name", "같은 이름의 광고주가 이미 있습니다."))

        // when & then
        mockMvc.perform(
            admin(patch("/api/v1/admin/advertisers/$ADVERTISER_ID"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("name" to "새싹"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("name: 같은 이름의 광고주가 이미 있습니다."))
    }

    private fun result() = AdminAdvertiserResult(
        id = ADVERTISER_ID,
        name = "그로우랩",
        type = AdvertiserType.NON_MEMBER,
        source = AdvertiserSource.MANUAL,
        companyMemberUserId = null,
        adInquiryId = null,
        contactName = "정하늘",
        contactEmail = null,
        contactPhone = null,
        adCount = AdvertiserAdCountDto.NONE,
        contractTotals = AdminAdvertiserContractTotals(
            contractCount = 2,
            activeContractCount = 1,
            contractTotalAmount = 3_000_000,
            contractVatMixed = true,
        ),
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val ADVERTISER_ID = 3L
    }
}
