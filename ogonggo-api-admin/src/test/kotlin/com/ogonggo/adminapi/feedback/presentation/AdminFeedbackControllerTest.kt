package com.ogonggo.adminapi.feedback.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.feedback.business.AdminFeedbackPageResult
import com.ogonggo.adminapi.feedback.business.AdminFeedbackService
import com.ogonggo.adminapi.feedback.business.AdminFeedbackSummary
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
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminFeedbackController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminFeedbackControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminFeedbackService: AdminFeedbackService

    @Test
    fun `관리자 토큰이 없으면 개선 의견 목록을 볼 수 없다`() {
        mockMvc.perform(get(PATH))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminFeedbackService)
    }

    @Test
    fun `개선 의견 목록은 1부터 시작하는 페이지로 주고 기본 크기는 20이다`() {
        // given
        val summary = AdminFeedbackSummary(
            id = 5L,
            userId = null,
            satisfaction = null,
            improvement = "알림이 필요해요",
            registeredAt = LocalDateTime.of(2026, 9, 28, 10, 0),
        )
        Mockito.`when`(adminFeedbackService.getFeedbacks(0, 20))
            .thenReturn(AdminFeedbackPageResult(listOf(summary), page = 0, size = 20, totalElements = 1, totalPages = 1))

        // when & then
        mockMvc.perform(get(PATH).with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(5))
            .andExpect(jsonPath("$.data.items[0].userId").doesNotExist())
            .andExpect(jsonPath("$.data.items[0].improvement").value("알림이 필요해요"))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
    }

    private companion object {
        const val PATH = "/api/v1/admin/feedbacks"
    }
}
