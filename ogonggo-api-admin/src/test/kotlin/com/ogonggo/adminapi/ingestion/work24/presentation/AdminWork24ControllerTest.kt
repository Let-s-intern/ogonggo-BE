package com.ogonggo.adminapi.ingestion.work24.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.ingestion.work24.business.AdminWork24Service
import com.ogonggo.adminapi.ingestion.work24.error.Work24ErrorCode
import com.ogonggo.adminapi.ingestion.work24.implement.Work24Api
import com.ogonggo.core.error.InternalServerException
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AdminWork24Controller::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminWork24ControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminWork24Service: AdminWork24Service

    @Test
    fun `관리자 토큰이 없으면 고용24를 조회할 수 없다`() {
        mockMvc.perform(get("/api/v1/admin/work24/recruitments"))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminWork24Service)
    }

    @Test
    fun `경로의 API 이름과 query 전체를 넘기고 응답을 data에 담는다`() {
        // given
        val parameters = mapOf("pageNum" to "1", "srchTraProcessNm" to "백엔드")
        Mockito.`when`(adminWork24Service.fetch(Work24Api.TOMORROW_LEARNING_CARD_COURSES, parameters))
            .thenReturn(objectMapper.readTree("""{"scn_cnt": 1}"""))

        // when & then
        mockMvc.perform(
            admin(
                get("/api/v1/admin/work24/tomorrow-learning-card-courses")
                    .param("pageNum", "1")
                    .param("srchTraProcessNm", "백엔드"),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.scn_cnt").value(1))
    }

    @Test
    fun `지원하지 않는 API 이름이면 apiName 오류로 알린다`() {
        mockMvc.perform(admin(get("/api/v1/admin/work24/TOMORROW_LEARNING_CARD_COURSES")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[apiName] 지원하지 않는 고용24 API입니다."))

        Mockito.verifyNoInteractions(adminWork24Service)
    }

    @Test
    fun `인증키가 설정되지 않은 서비스는 503으로 알린다`() {
        Mockito.`when`(adminWork24Service.fetch(Work24Api.RECRUITMENTS, emptyMap()))
            .thenThrow(InternalServerException(Work24ErrorCode.WORK24_AUTH_KEY_NOT_CONFIGURED))

        mockMvc.perform(admin(get("/api/v1/admin/work24/recruitments")))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("WORK24_AUTH_KEY_NOT_CONFIGURED"))
    }

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))
}
