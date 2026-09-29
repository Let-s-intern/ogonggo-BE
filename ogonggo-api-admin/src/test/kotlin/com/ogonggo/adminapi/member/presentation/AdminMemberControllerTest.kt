package com.ogonggo.adminapi.member.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.member.business.AdminMemberService
import com.ogonggo.core.user.domain.UserManagementSearchCondition
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.dto.CompanyMemberPageDto
import com.ogonggo.core.user.implement.dto.GeneralMemberDto
import com.ogonggo.core.user.implement.dto.GeneralMemberPageDto
import com.ogonggo.core.user.implement.dto.UserProfileDto
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
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminGeneralMemberController::class, AdminCompanyMemberController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminMemberControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminMemberService: AdminMemberService

    @Test
    fun `관리자 토큰이 없으면 회원 목록을 볼 수 없다`() {
        mockMvc.perform(get("/api/v1/admin/general-members"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/admin/company-members"))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminMemberService)
    }

    @Test
    fun `일반 회원 목록은 검색어·상태·가입 기간을 조건으로 옮기고 프로필을 펼쳐 준다`() {
        // given
        val condition = UserManagementSearchCondition(
            status = UserStatus.ACTIVE,
            joinedFrom = LocalDate.of(2026, 9, 1),
            joinedTo = LocalDate.of(2026, 9, 28),
            keyword = "렛츠",
        )
        Mockito.`when`(adminMemberService.getGeneralMembers(condition, 0, 20)).thenReturn(
            GeneralMemberPageDto(
                members = listOf(generalMember()),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1,
            ),
        )

        // when & then
        mockMvc.perform(
            admin(
                get("/api/v1/admin/general-members")
                    .param("keyword", "렛츠")
                    .param("status", "ACTIVE")
                    .param("joinedFrom", "2026-09-01")
                    .param("joinedTo", "2026-09-28"),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].userId").value(USER_ID))
            .andExpect(jsonPath("$.data.items[0].nickname").value("렛츠"))
            .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
    }

    @Test
    fun `빈 필터 값은 보내지 않은 것과 같다`() {
        Mockito.`when`(adminMemberService.getCompanyMembers(UserManagementSearchCondition(), 0, 20))
            .thenReturn(CompanyMemberPageDto(emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0))

        mockMvc.perform(
            admin(
                get("/api/v1/admin/company-members")
                    .param("keyword", " ")
                    .param("status", "")
                    .param("joinedFrom", "")
                    .param("joinedTo", ""),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0))
    }

    @Test
    fun `가입 기간 시작일이 종료일보다 늦으면 joinedFrom 파라미터 오류로 알린다`() {
        mockMvc.perform(
            admin(
                get("/api/v1/admin/company-members")
                    .param("joinedFrom", "2026-09-28")
                    .param("joinedTo", "2026-09-01"),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("[joinedFrom] 가입 기간 시작일은 종료일보다 늦을 수 없습니다."))

        Mockito.verifyNoInteractions(adminMemberService)
    }

    private fun generalMember(): GeneralMemberDto = GeneralMemberDto(
        userId = USER_ID,
        letsCareerUserId = 4821L,
        status = UserStatus.ACTIVE,
        joinedAt = NOW,
        withdrawnAt = null,
        profile = UserProfileDto(
            name = "김렛츠",
            email = "lets@test.com",
            phoneNum = null,
            letsCareerAuthProvider = null,
            notificationEmail = null,
            nickname = "렛츠",
            profileImageUrl = null,
            university = null,
            major = null,
            grade = null,
            wishField = null,
            wishJob = null,
            wishIndustry = null,
            wishEmploymentType = null,
            wishCompany = null,
        ),
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private companion object {
        const val USER_ID = 7L
        val NOW: LocalDateTime = LocalDateTime.of(2026, 9, 28, 10, 0)
    }
}
