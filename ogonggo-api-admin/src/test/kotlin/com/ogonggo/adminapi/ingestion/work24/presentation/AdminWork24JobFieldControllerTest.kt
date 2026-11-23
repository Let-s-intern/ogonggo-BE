package com.ogonggo.adminapi.ingestion.work24.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.ingestion.work24.business.AdminWork24JobFieldService
import com.ogonggo.core.job.domain.JobField
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

@WebMvcTest(controllers = [AdminWork24JobFieldController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminWork24JobFieldControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminWork24JobFieldService: AdminWork24JobFieldService

    @Test
    fun `관리자 토큰이 없으면 수집 직군을 보거나 바꿀 수 없다`() {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized)
        mockMvc.perform(putJson("SALES", """{"enabled": false}""")).andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminWork24JobFieldService)
    }

    @Test
    fun `직군마다 수집 켜짐 여부를 직군 순서대로 data에 담아 준다`() {
        Mockito.`when`(adminWork24JobFieldService.findAll())
            .thenReturn(linkedMapOf(JobField.IT_DEVELOPMENT to true, JobField.GAME to false))

        mockMvc.perform(admin(get(PATH)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].jobField").value("IT_DEVELOPMENT"))
            .andExpect(jsonPath("$.data[0].enabled").value(true))
            .andExpect(jsonPath("$.data[1].jobField").value("GAME"))
            .andExpect(jsonPath("$.data[1].enabled").value(false))
    }

    @Test
    fun `직군 하나를 끄고 바뀐 값을 돌려준다`() {
        Mockito.`when`(adminWork24JobFieldService.findAll()).thenReturn(mapOf(JobField.SALES to false))

        mockMvc.perform(admin(putJson("SALES", """{"enabled": false}""")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.jobField").value("SALES"))
            .andExpect(jsonPath("$.data.enabled").value(false))

        Mockito.verify(adminWork24JobFieldService).replace(JobField.SALES, false)
    }

    @Test
    fun `없는 직군이거나 enabled가 빠졌거나 참·거짓이 아니면 400으로 응답하고 바꾸지 않는다`() {
        mockMvc.perform(admin(putJson("UNKNOWN", """{"enabled": false}""")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        listOf("{}", """{"enabled": null}""", """{"enabled": "maybe"}""").forEach { body ->
            mockMvc.perform(admin(putJson("SALES", body)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(adminWork24JobFieldService)
    }

    private fun putJson(jobField: String, body: String): MockHttpServletRequestBuilder =
        put("$PATH/$jobField").contentType(MediaType.APPLICATION_JSON).content(body)

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val PATH = "/api/v1/admin/work24-job-fields"
    }
}
