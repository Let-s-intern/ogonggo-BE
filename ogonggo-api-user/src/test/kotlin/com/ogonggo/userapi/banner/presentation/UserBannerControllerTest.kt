package com.ogonggo.userapi.banner.presentation

import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.banner.business.UserBannerResult
import com.ogonggo.userapi.banner.business.UserBannerService
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UserBannerController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserBannerControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userBannerService: UserBannerService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인 없이 자리의 노출 중인 배너를 운영 칸 없이 준다`() {
        // given
        Mockito.`when`(userBannerService.getBanners(BannerPlacement.HOME_TOP)).thenReturn(
            listOf(
                UserBannerResult(
                    id = 1,
                    placement = BannerPlacement.HOME_TOP,
                    pcImageUrl = "/banners/jobs-pc.svg",
                    mobileImageUrl = null,
                    linkUrl = "/calendar",
                    openInNewTab = false,
                    altText = "매일 아침 올라오는 새 공고",
                    showAdLabel = false,
                ),
            ),
        )

        // when & then
        mockMvc.perform(get("/api/v1/banners").param("placement", "HOME_TOP"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].placement").value("HOME_TOP"))
            .andExpect(jsonPath("$.data[0].pcImageUrl").value("/banners/jobs-pc.svg"))
            .andExpect(jsonPath("$.data[0].mobileImageUrl").isEmpty)
            .andExpect(jsonPath("$.data[0].showAdLabel").value(false))
            .andExpect(jsonPath("$.data[0].name").doesNotExist())
            .andExpect(jsonPath("$.data[0].displayOrder").doesNotExist())
            .andExpect(jsonPath("$.data[0].enabled").doesNotExist())
    }

    @Test
    fun `자리가 없거나 모르는 값이면 400이다`() {
        mockMvc.perform(get("/api/v1/banners"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        mockMvc.perform(get("/api/v1/banners").param("placement", "SIDEBAR"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }
}
