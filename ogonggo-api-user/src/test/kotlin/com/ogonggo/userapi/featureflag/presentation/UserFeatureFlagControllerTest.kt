package com.ogonggo.userapi.featureflag.presentation

import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.featureflag.business.UserFeatureFlagResult
import com.ogonggo.userapi.featureflag.business.UserFeatureFlagService
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

@WebMvcTest(controllers = [UserFeatureFlagController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserFeatureFlagControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userFeatureFlagService: UserFeatureFlagService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인 없이 기능 플래그 목록을 조회하고 설명과 변경자는 응답에 없다`() {
        // given
        Mockito.`when`(userFeatureFlagService.getFeatureFlags()).thenReturn(
            listOf(
                UserFeatureFlagResult(key = "ad_report_cost_efficiency", enabled = true),
                UserFeatureFlagResult(key = "other_flag", enabled = false),
            ),
        )

        // when & then
        mockMvc.perform(get(PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.items[0].key").value("ad_report_cost_efficiency"))
            .andExpect(jsonPath("$.data.items[0].enabled").value(true))
            .andExpect(jsonPath("$.data.items[1].key").value("other_flag"))
            .andExpect(jsonPath("$.data.items[1].enabled").value(false))
            .andExpect(jsonPath("$.data.items[0].description").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].updatedBy").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].updatedAt").doesNotExist())
    }

    @Test
    fun `플래그가 없으면 빈 목록을 준다`() {
        Mockito.`when`(userFeatureFlagService.getFeatureFlags()).thenReturn(emptyList())

        mockMvc.perform(get(PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items.length()").value(0))
    }

    private companion object {
        const val PATH = "/api/v1/feature-flags"
    }
}
