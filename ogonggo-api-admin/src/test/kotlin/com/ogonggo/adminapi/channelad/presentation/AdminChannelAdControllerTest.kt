package com.ogonggo.adminapi.channelad.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.channelad.business.AdminChannelAdCreateCommand
import com.ogonggo.adminapi.channelad.business.AdminChannelAdResult
import com.ogonggo.adminapi.channelad.business.AdminChannelAdSearchCommand
import com.ogonggo.adminapi.channelad.business.AdminChannelAdService
import com.ogonggo.adminapi.channelad.business.AdminChannelAdUpdateCommand
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.channelad.domain.ChannelAdStatus
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(controllers = [AdminChannelAdController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminChannelAdControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminChannelAdService: AdminChannelAdService

    @Test
    fun `목록은 달과 거르기 조건을 받아 광고주·계약·채널·유형 요약을 실은 배열을 준다`() {
        // given
        val command = AdminChannelAdSearchCommand(
            month = "2026-11",
            advertiserId = 1,
            platform = AdChannelPlatform.INSTAGRAM,
            status = ChannelAdStatus.SCHEDULED,
        )
        Mockito.`when`(adminChannelAdService.getChannelAds(command)).thenReturn(listOf(result()))

        // when & then
        mockMvc.perform(
            admin(get("/api/v1/admin/channel-ads"))
                .param("month", "2026-11")
                .param("advertiserId", "1")
                .param("platform", "INSTAGRAM")
                .param("status", "SCHEDULED"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(AD_ID))
            .andExpect(jsonPath("$.data[0].advertiser.type").value("NON_MEMBER"))
            .andExpect(jsonPath("$.data[0].contract.startAt").value("2026-11-01T00:00:00"))
            .andExpect(jsonPath("$.data[0].channel.handle").value("letscareer.job"))
            .andExpect(jsonPath("$.data[0].format.name").value("릴스(숏츠)"))
            .andExpect(jsonPath("$.data[0].postDate").value("2026-11-10"))
            .andExpect(jsonPath("$.data[0].endDate").isEmpty)
    }

    @Test
    fun `등록하면 201과 등록한 광고를 준다`() {
        // given
        val command = AdminChannelAdCreateCommand(
            advertiserId = 1,
            contractId = 3,
            channelId = 1,
            formatId = 2,
            postDate = LocalDate.of(2026, 11, 10),
            endDate = null,
            status = null,
            postUrl = null,
            creativeMemo = "문구",
            adminMemo = null,
        )
        Mockito.`when`(adminChannelAdService.createChannelAd(command)).thenReturn(AD_ID)
        Mockito.`when`(adminChannelAdService.getChannelAd(AD_ID)).thenReturn(result())

        // when & then
        mockMvc.perform(
            admin(post("/api/v1/admin/channel-ads"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"advertiserId":1,"contractId":3,"channelId":1,"formatId":2,"postDate":"2026-11-10","creativeMemo":"문구"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
    }

    @Test
    fun `수정은 한 칸씩 보낸 값만 바꾸고 null을 보낸 칸은 지운다`() {
        // given
        Mockito.`when`(adminChannelAdService.getChannelAd(AD_ID)).thenReturn(result())

        // when
        mockMvc.perform(
            admin(patch("/api/v1/admin/channel-ads/$AD_ID"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"PUBLISHED","contractId":null}"""),
        )
            .andExpect(status().isOk)

        // then
        Mockito.verify(adminChannelAdService)
            .updateChannelAd(AD_ID, AdminChannelAdUpdateCommand(status = ChannelAdStatus.PUBLISHED, contractId = Optional.empty()))
    }

    private fun result() = AdminChannelAdResult(
        id = AD_ID,
        advertiserId = 1,
        advertiser = AdminChannelAdResult.AdvertiserSummary(1, "그로우랩", AdvertiserType.NON_MEMBER),
        contractId = 3,
        contract = AdminChannelAdResult.ContractSummary(3, "11월", START, START.plusMonths(1)),
        platform = AdChannelPlatform.INSTAGRAM,
        channelId = 1,
        channel = AdminChannelAdResult.ChannelSummary(1, AdChannelPlatform.INSTAGRAM, "렛츠커리어 오공고", "letscareer.job"),
        formatId = 2,
        format = AdminChannelAdResult.FormatSummary(2, AdChannelPlatform.INSTAGRAM, "릴스(숏츠)"),
        postDate = LocalDate.of(2026, 11, 10),
        endDate = null,
        status = ChannelAdStatus.SCHEDULED,
        postUrl = null,
        creativeMemo = "문구",
        adminMemo = null,
        createdAt = START,
        updatedAt = START,
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val AD_ID = 5L
        val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 0, 0)
    }
}
