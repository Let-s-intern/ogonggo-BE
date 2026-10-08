package com.ogonggo.adminapi.concern.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.concern.business.AdminConcernDetail
import com.ogonggo.adminapi.concern.business.AdminConcernPageResult
import com.ogonggo.adminapi.concern.business.AdminConcernService
import com.ogonggo.adminapi.concern.business.AdminConcernVisibilityChangeCommand
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernConsoleSearchCondition
import com.ogonggo.core.concern.domain.ConcernSortType
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
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminConcernController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminConcernControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminConcernService: AdminConcernService

    @Test
    fun `노출·카테고리·검색어 필터와 조회수 정렬을 조회 조건으로 옮긴다`() {
        // given
        val condition = ConcernConsoleSearchCondition(
            visible = false,
            category = ConcernCategory.CAREER,
            keyword = "면접",
        )
        Mockito.`when`(adminConcernService.getConcerns(condition, ConcernSortType.VIEW_COUNT, 1, 10))
            .thenReturn(AdminConcernPageResult(emptyList(), page = 1, size = 10, totalElements = 0, totalPages = 0))

        // when & then
        mockMvc.perform(
            admin(
                get("/api/v1/admin/concerns")
                    .param("page", "2")
                    .param("size", "10")
                    .param("sort", "VIEW_COUNT")
                    .param("visibility", "HIDDEN")
                    .param("category", "CAREER")
                    .param("keyword", "면접"),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(2))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))
    }

    @Test
    fun `상세는 숨긴 고민글의 본문과 노출 여부를 준다`() {
        // given
        Mockito.`when`(adminConcernService.getConcern(7L)).thenReturn(
            AdminConcernDetail(
                id = 7L,
                category = ConcernCategory.ETC,
                title = "제목",
                content = "본문",
                viewCount = 3,
                commentCount = 1,
                hasOfficialComment = false,
                visibility = AdminContentVisibility.HIDDEN,
                authorUserId = 17L,
                authorNickname = null,
                registeredAt = NOW,
                updatedAt = NOW,
            ),
        )

        // when & then
        mockMvc.perform(admin(get("/api/v1/admin/concerns/7")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").value("본문"))
            .andExpect(jsonPath("$.data.visibility").value("HIDDEN"))
            .andExpect(jsonPath("$.data.authorUserId").value(17))
    }

    @Test
    fun `고른 고민글의 노출을 한꺼번에 바꾸고 data 없이 응답한다`() {
        mockMvc.perform(
            admin(
                patch("/api/v1/admin/concerns/visibility")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [5, 2], "visibility": "HIDDEN"}"""),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(adminConcernService)
            .changeVisibilities(AdminConcernVisibilityChangeCommand(listOf(5L, 2L), AdminContentVisibility.HIDDEN))
    }

    @Test
    fun `양수가 아닌 고민글 식별자가 있으면 아무것도 바꾸지 않고 거절한다`() {
        mockMvc.perform(
            admin(
                patch("/api/v1/admin/concerns/visibility")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [5, 0], "visibility": "VISIBLE"}"""),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[ids] 고민글 식별자는 양수여야 합니다."))

        Mockito.verifyNoInteractions(adminConcernService)
    }

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    companion object {
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 8, 12, 0)
    }
}
