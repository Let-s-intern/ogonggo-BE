package com.ogonggo.adminapi.enumeration.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.enumeration.business.AdminEnumService
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.enumeration.EnumOption
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AdminEnumController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminEnumControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminEnumService: AdminEnumService

    @Test
    fun `관리자 토큰이 없으면 선택지를 볼 수 없다`() {
        mockMvc.perform(get(PATH))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminEnumService)
    }

    @Test
    fun `enum 이름별 선택지를 이름과 라벨, 상위 값으로 조회한다`() {
        // given
        Mockito.`when`(adminEnumService.getEnums()).thenReturn(
            mapOf(
                "JobEmploymentType" to listOf(EnumOption(name = "FULL_TIME", desc = "정규직")),
                "JobRole" to listOf(EnumOption(name = "IT_BACKEND", desc = "백엔드", parent = "IT_DEVELOPMENT")),
            ),
        )

        // when & then
        mockMvc.perform(get(PATH).with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.JobEmploymentType[0].name").value("FULL_TIME"))
            .andExpect(jsonPath("$.data.JobEmploymentType[0].desc").value("정규직"))
            .andExpect(jsonPath("$.data.JobEmploymentType[0].code").doesNotExist())
            .andExpect(jsonPath("$.data.JobEmploymentType[0].parent").isEmpty)
            .andExpect(jsonPath("$.data.JobRole[0].parent").value("IT_DEVELOPMENT"))
    }

    private companion object {
        const val PATH = "/api/v1/admin/enums"
    }
}
