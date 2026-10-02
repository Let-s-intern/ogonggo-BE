package com.ogonggo.adminapi.community.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.community.business.AdminRecruitmentPostPageResult
import com.ogonggo.adminapi.community.business.AdminRecruitmentPostService
import com.ogonggo.adminapi.community.business.AdminRecruitmentPostVisibilityChangeCommand
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.community.domain.RecruitmentPostConsoleSearchCondition
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
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

@WebMvcTest(controllers = [AdminRecruitmentPostController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminRecruitmentPostControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminRecruitmentPostService: AdminRecruitmentPostService

    @Test
    fun `노출·모집 구분·모집 상태·검색어 필터와 조회수 정렬을 조회 조건으로 옮긴다`() {
        // given
        val condition = RecruitmentPostConsoleSearchCondition(
            published = false,
            recruitmentType = RecruitmentType.STUDY,
            recruitmentStatus = RecruitmentStatus.CLOSED,
            keyword = "코틀린",
        )
        Mockito.`when`(
            adminRecruitmentPostService.getRecruitmentPosts(condition, RecruitmentPostSortType.VIEW_COUNT, 1, 10),
        ).thenReturn(AdminRecruitmentPostPageResult(emptyList(), page = 1, size = 10, totalElements = 0, totalPages = 0))

        // when & then
        mockMvc.perform(
            admin(
                get("/api/v1/admin/recruitment-posts")
                    .param("page", "2")
                    .param("size", "10")
                    .param("sort", "VIEW_COUNT")
                    .param("visibility", "HIDDEN")
                    .param("recruitmentType", "STUDY")
                    .param("recruitmentStatus", "CLOSED")
                    .param("keyword", "코틀린"),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(2))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))
    }

    @Test
    fun `고른 모집글의 노출을 한꺼번에 바꾸고 data 없이 응답한다`() {
        mockMvc.perform(
            admin(
                patch("/api/v1/admin/recruitment-posts/visibility")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [5, 2], "visibility": "HIDDEN"}"""),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(adminRecruitmentPostService)
            .changeVisibilities(AdminRecruitmentPostVisibilityChangeCommand(listOf(5L, 2L), AdminContentVisibility.HIDDEN))
    }

    @Test
    fun `양수가 아닌 모집글 식별자가 있으면 아무것도 바꾸지 않고 거절한다`() {
        mockMvc.perform(
            admin(
                patch("/api/v1/admin/recruitment-posts/visibility")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [5, 0], "visibility": "VISIBLE"}"""),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[ids] 모집글 식별자는 양수여야 합니다."))

        Mockito.verifyNoInteractions(adminRecruitmentPostService)
    }

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))
}
