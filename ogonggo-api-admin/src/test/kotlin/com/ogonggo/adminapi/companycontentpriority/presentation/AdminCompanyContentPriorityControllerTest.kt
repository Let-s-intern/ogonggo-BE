package com.ogonggo.adminapi.companycontentpriority.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.companycontentpriority.business.AdminCompanyContentPriorityService
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AdminCompanyContentPriorityController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminCompanyContentPriorityControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminCompanyContentPriorityService: AdminCompanyContentPriorityService

    @Test
    fun `관리자 토큰이 없으면 우선 노출 설정을 보거나 바꿀 수 없다`() {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized)
        mockMvc.perform(putJson("""{"enabled": false}""")).andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminCompanyContentPriorityService)
    }

    @Test
    fun `우선 노출 켜짐 여부를 data에 담아 준다`() {
        Mockito.`when`(adminCompanyContentPriorityService.isEnabled()).thenReturn(true)

        mockMvc.perform(admin(get(PATH)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.enabled").value(true))
    }

    @Test
    fun `우선 노출을 바꾸고 바뀐 값을 돌려준다`() {
        Mockito.`when`(adminCompanyContentPriorityService.isEnabled()).thenReturn(false)

        mockMvc.perform(admin(putJson("""{"enabled": false}""")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.enabled").value(false))

        Mockito.verify(adminCompanyContentPriorityService).replace(false)
    }

    @Test
    fun `enabled가 빠졌거나 참·거짓이 아니면 400으로 응답하고 바꾸지 않는다`() {
        listOf("{}", """{"enabled": null}""", """{"enabled": "maybe"}""").forEach { body ->
            mockMvc.perform(admin(putJson(body)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(adminCompanyContentPriorityService)
    }

    private fun putJson(body: String): MockHttpServletRequestBuilder =
        put(PATH).contentType(MediaType.APPLICATION_JSON).content(body)

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val PATH = "/api/v1/admin/company-content-priority"
    }
}
