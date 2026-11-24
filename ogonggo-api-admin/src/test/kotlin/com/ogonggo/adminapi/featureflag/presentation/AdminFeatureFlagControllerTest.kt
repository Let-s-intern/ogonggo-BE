package com.ogonggo.adminapi.featureflag.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.featureflag.business.AdminFeatureFlagResult
import com.ogonggo.adminapi.featureflag.business.AdminFeatureFlagService
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.featureflag.error.FeatureFlagErrorCode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminFeatureFlagController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminFeatureFlagControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminFeatureFlagService: AdminFeatureFlagService

    @Test
    fun `관리자 토큰이 없으면 기능 플래그를 보거나 바꿀 수 없다`() {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized)
        mockMvc.perform(patchJson("$PATH/$KEY", """{"enabled": false}""")).andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminFeatureFlagService)
    }

    @Test
    fun `관리자 권한이 없으면 기능 플래그는 403이다`() {
        listOf(get(PATH), patchJson("$PATH/$KEY", """{"enabled": false}""")).forEach { request ->
            mockMvc.perform(request.with(user("member")))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        }

        Mockito.verifyNoInteractions(adminFeatureFlagService)
    }

    @Test
    fun `목록은 설명과 변경 일시와 변경자를 함께 준다`() {
        // given
        Mockito.`when`(adminFeatureFlagService.getFeatureFlags()).thenReturn(
            listOf(
                AdminFeatureFlagResult("ad_report_cost_efficiency", true, "비용 효율 섹션을 보인다", LocalDateTime.of(2026, 11, 24, 10, 0), null),
                AdminFeatureFlagResult("other_flag", false, "다른 플래그", LocalDateTime.of(2026, 11, 24, 11, 30), ADMIN_ID),
            ),
        )

        // when & then
        mockMvc.perform(admin(get(PATH)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.items[0].key").value("ad_report_cost_efficiency"))
            .andExpect(jsonPath("$.data.items[0].enabled").value(true))
            .andExpect(jsonPath("$.data.items[0].description").value("비용 효율 섹션을 보인다"))
            .andExpect(jsonPath("$.data.items[0].updatedAt").value("2026-11-24T10:00:00"))
            .andExpect(jsonPath("$.data.items[0].updatedBy").doesNotExist())
            .andExpect(jsonPath("$.data.items[1].enabled").value(false))
            .andExpect(jsonPath("$.data.items[1].updatedBy").value(ADMIN_ID))
    }

    @Test
    fun `켜짐 여부를 바꾸면 요청한 관리자와 key를 서비스에 넘기고 data는 null이다`() {
        mockMvc.perform(admin(patchJson("$PATH/$KEY", """{"enabled": false}""")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(adminFeatureFlagService).changeFeatureFlagEnabled(ADMIN_ID, KEY, false)
    }

    @Test
    fun `없는 key는 404 FEATURE_FLAG_NOT_FOUND이다`() {
        Mockito.`when`(adminFeatureFlagService.changeFeatureFlagEnabled(ADMIN_ID, "missing_flag", true))
            .thenThrow(EntityNotFoundException(FeatureFlagErrorCode.FEATURE_FLAG_NOT_FOUND))

        mockMvc.perform(admin(patchJson("$PATH/missing_flag", """{"enabled": true}""")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("FEATURE_FLAG_NOT_FOUND"))
    }

    @Test
    fun `enabled가 빠졌거나 참·거짓이 아니면 400으로 응답하고 바꾸지 않는다`() {
        listOf("{}", """{"enabled": null}""", """{"enabled": "maybe"}""").forEach { body ->
            mockMvc.perform(admin(patchJson("$PATH/$KEY", body)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(adminFeatureFlagService)
    }

    private fun patchJson(path: String, body: String): MockHttpServletRequestBuilder =
        patch(path).contentType(MediaType.APPLICATION_JSON).content(body)

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(
            authentication(
                UsernamePasswordAuthenticationToken(ADMIN_ID, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY))),
            ),
        )

    private companion object {
        const val PATH = "/api/v1/admin/feature-flags"
        const val KEY = "ad_report_cost_efficiency"
        const val ADMIN_ID = 7L
    }
}
