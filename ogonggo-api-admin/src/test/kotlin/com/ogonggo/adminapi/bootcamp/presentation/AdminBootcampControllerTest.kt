package com.ogonggo.adminapi.bootcamp.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.bootcamp.business.AdminBootcampPageResult
import com.ogonggo.adminapi.bootcamp.business.AdminBootcampService
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.bootcamp.domain.BootcampManagementSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.review.domain.ContentSource
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AdminBootcampController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminBootcampControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminBootcampService: AdminBootcampService

    @Test
    fun `모집 상태와 등록 경로 필터를 조회 조건으로 옮긴다`() {
        val condition = BootcampManagementSearchCondition(
            source = ContentSource.CRAWLER,
            status = BootcampStatus.CLOSED,
        )
        Mockito.`when`(adminBootcampService.getBootcamps(condition, BootcampSortType.LATEST, 0, 20))
            .thenReturn(AdminBootcampPageResult(emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0))

        mockMvc.perform(
            admin(get("/api/v1/admin/bootcamps").param("source", "CRAWLER").param("status", "CLOSED")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
    }

    @Test
    fun `임시저장 모집 상태로는 거를 수 없다`() {
        mockMvc.perform(admin(get("/api/v1/admin/bootcamps").param("status", "DRAFT")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[status] RECRUITING 또는 CLOSED만 고를 수 있습니다."))

        Mockito.verifyNoInteractions(adminBootcampService)
    }

    @Test
    fun `상세 내용은 비울 수 없다`() {
        mockMvc.perform(
            admin(
                patch("/api/v1/admin/bootcamps/3")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("fields" to mapOf("content" to "")))),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[fields.content] 상세 내용은 비울 수 없습니다."))

        Mockito.verifyNoInteractions(adminBootcampService)
    }

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))
}
