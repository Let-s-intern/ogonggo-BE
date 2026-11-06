package com.ogonggo.adminapi.banner.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.banner.business.AdminBannerAdvertiserResult
import com.ogonggo.adminapi.banner.business.AdminBannerContractResult
import com.ogonggo.adminapi.banner.business.AdminBannerCreateCommand
import com.ogonggo.adminapi.banner.business.AdminBannerResult
import com.ogonggo.adminapi.banner.business.AdminBannerService
import com.ogonggo.adminapi.banner.business.AdminBannerUpdateCommand
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.banner.domain.BannerPlacement
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(controllers = [AdminBannerController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminBannerControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminBannerService: AdminBannerService

    @Test
    fun `목록은 자리나 광고주로 거르고 둘 다 없거나 값이 틀리면 화면 문구로 400이다`() {
        // given
        Mockito.`when`(adminBannerService.getBanners(null, ADVERTISER_ID)).thenReturn(listOf(result()))

        // when & then
        mockMvc.perform(admin(get(PATH)).param("advertiserId", "$ADVERTISER_ID"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].advertiser.name").value("오공고 광고주"))
            .andExpect(jsonPath("$.data[0].contract.startAt").value("2026-11-01T00:00:00"))
        mockMvc.perform(admin(get(PATH)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(PLACEMENT_MESSAGE))
        mockMvc.perform(admin(get(PATH)).param("placement", "SIDEBAR"))
            .andExpect(jsonPath("$.message").value(PLACEMENT_MESSAGE))
        mockMvc.perform(admin(get(PATH)).param("placement", "HOME_TOP").param("advertiserId", "abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("advertiserId: 광고주 id 를 숫자로 보내 주세요."))
    }

    @Test
    fun `등록하면 201과 운영 칸을 포함한 배너를 주고 광고주·계약이 없으면 null을 싣는다`() {
        // given
        val command = AdminBannerCreateCommand(
            placement = BannerPlacement.HOME_TOP,
            name = "홈 배너",
            pcImageUrl = "/banners/jobs-pc.svg",
            mobileImageUrl = null,
            linkUrl = "/calendar",
            openInNewTab = false,
            altText = "새 공고",
            showAdLabel = false,
            startAt = null,
            endAt = LocalDateTime.of(2026, 12, 1, 0, 0),
            enabled = true,
            advertiserId = null,
            contractId = null,
        )
        Mockito.`when`(adminBannerService.createBanner(command)).thenReturn(BANNER_ID)
        Mockito.`when`(adminBannerService.getBanner(BANNER_ID)).thenReturn(result(advertiser = null, contract = null))

        // when & then
        mockMvc.perform(
            admin(post(PATH))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"placement":"HOME_TOP","name":"홈 배너","pcImageUrl":"/banners/jobs-pc.svg","mobileImageUrl":null,
                     "linkUrl":"/calendar","openInNewTab":false,"altText":"새 공고","showAdLabel":false,
                     "startAt":null,"endAt":"2026-12-01T00:00:00","enabled":true,"advertiserId":null,"contractId":null}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(BANNER_ID))
            .andExpect(jsonPath("$.data.displayOrder").value(1))
            .andExpect(jsonPath("$.data.name").value("홈 배너"))
            .andExpect(jsonPath("$.data.advertiser").isEmpty)
            .andExpect(jsonPath("$.data.contract").isEmpty)
            .andExpect(jsonPath("$.data.createdAt").value("2026-11-01T09:00:00"))
    }

    @Test
    fun `수정은 null을 보낸 칸은 비우고 보내지 않은 칸은 그대로 둔다`() {
        // given
        Mockito.`when`(adminBannerService.getBanner(BANNER_ID)).thenReturn(result())

        // when
        mockMvc.perform(
            admin(patch("$PATH/$BANNER_ID"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":false,"contractId":null}"""),
        )
            .andExpect(status().isOk)

        // then
        Mockito.verify(adminBannerService).updateBanner(BANNER_ID, AdminBannerUpdateCommand(enabled = false, contractId = Optional.empty()))
    }

    @Test
    fun `순서 바꾸기는 그 자리 배너를 새 순서로 주고 삭제는 지운 id를 준다`() {
        // given
        Mockito.`when`(adminBannerService.getBanners(BannerPlacement.HOME_TOP, null)).thenReturn(listOf(result()))

        // when & then
        mockMvc.perform(
            admin(put("$PATH/order"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"placement":"HOME_TOP","bannerIds":[$BANNER_ID]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(BANNER_ID))
        Mockito.verify(adminBannerService).replaceOrder(BannerPlacement.HOME_TOP, listOf(BANNER_ID))
        mockMvc.perform(admin(delete("$PATH/$BANNER_ID")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(BANNER_ID))
    }

    @Test
    fun `관리자가 아니면 배너를 볼 수 없다`() {
        mockMvc.perform(get(PATH).param("placement", "HOME_TOP"))
            .andExpect(status().isUnauthorized)
    }

    private fun admin(builder: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        builder.with(user("1").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private fun result(
        advertiser: AdminBannerAdvertiserResult? = AdminBannerAdvertiserResult(ADVERTISER_ID, "오공고 광고주", AdvertiserType.NON_MEMBER),
        contract: AdminBannerContractResult? = AdminBannerContractResult(
            id = 5,
            name = "11월 홈 배너",
            startAt = LocalDateTime.of(2026, 11, 1, 0, 0),
            endAt = LocalDateTime.of(2026, 12, 1, 0, 0),
        ),
    ) = AdminBannerResult(
        id = BANNER_ID,
        placement = BannerPlacement.HOME_TOP,
        name = "홈 배너",
        pcImageUrl = "/banners/jobs-pc.svg",
        mobileImageUrl = null,
        linkUrl = "/calendar",
        openInNewTab = false,
        altText = "새 공고",
        showAdLabel = advertiser != null,
        startAt = null,
        endAt = LocalDateTime.of(2026, 12, 1, 0, 0),
        enabled = true,
        displayOrder = 1,
        advertiserId = advertiser?.id,
        advertiser = advertiser,
        contractId = contract?.id,
        contract = contract,
        createdAt = LocalDateTime.of(2026, 11, 1, 9, 0),
        updatedAt = LocalDateTime.of(2026, 11, 1, 9, 0),
    )

    companion object {
        private const val PATH = "/api/v1/admin/banners"
        private const val BANNER_ID = 3L
        private const val ADVERTISER_ID = 10L
        private const val PLACEMENT_MESSAGE =
            "placement: HOME_TOP, HOME_BOTTOM, JOB_DETAIL_SIDE, BOOTCAMP_TOP, BOOTCAMP_BOTTOM, BOOTCAMP_DETAIL_SIDE 중 하나를 보내 주세요."
    }
}
