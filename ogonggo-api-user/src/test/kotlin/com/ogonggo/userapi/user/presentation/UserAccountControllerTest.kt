package com.ogonggo.userapi.user.presentation

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.user.business.MyAccountResult
import com.ogonggo.userapi.user.business.MyCompanyProfileResult
import com.ogonggo.userapi.user.business.MyProfileResult
import com.ogonggo.userapi.user.business.UserAccountService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [UserAccountController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserAccountControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userAccountService: UserAccountService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `일반 회원은 역할과 렛츠커리어 프로필을 받는다`() {
        Mockito.`when`(userAccountService.getMyAccount(USER_ID)).thenReturn(
            MyAccountResult(
                userId = USER_ID,
                role = UserRole.USER,
                status = UserStatus.ACTIVE,
                email = "lets@career.co.kr",
                joinedAt = JOINED_AT,
                profile = MyProfileResult("김렛츠", "렛츠", "https://example.com/me.png"),
                companyProfile = null,
            ),
        )

        mockMvc.perform(get("/api/v1/users/me").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.userId").value(USER_ID))
            .andExpect(jsonPath("$.data.role").value("USER"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.email").value("lets@career.co.kr"))
            .andExpect(jsonPath("$.data.profile.nickname").value("렛츠"))
            .andExpect(jsonPath("$.data.companyProfile").isEmpty)
    }

    @Test
    fun `기업 회원은 기업 정보를 받는다`() {
        Mockito.`when`(userAccountService.getMyAccount(USER_ID)).thenReturn(
            MyAccountResult(
                userId = USER_ID,
                role = UserRole.COMPANY,
                status = UserStatus.ACTIVE,
                email = "company@example.com",
                joinedAt = JOINED_AT,
                profile = null,
                companyProfile = MyCompanyProfileResult("렛츠커리어", "김담당"),
            ),
        )

        mockMvc.perform(get("/api/v1/users/me").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.role").value("COMPANY"))
            .andExpect(jsonPath("$.data.companyProfile.organizationName").value("렛츠커리어"))
            .andExpect(jsonPath("$.data.companyProfile.managerName").value("김담당"))
            .andExpect(jsonPath("$.data.profile").isEmpty)
    }

    @Test
    fun `인증이 없으면 401로 응답한다`() {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `계정이 없으면 도메인 404 계약으로 응답한다`() {
        Mockito.`when`(userAccountService.getMyAccount(USER_ID))
            .thenThrow(EntityNotFoundException(UserErrorCode.USER_NOT_FOUND))

        mockMvc.perform(get("/api/v1/users/me").with(authenticatedUser()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    companion object {
        private const val USER_ID = 17L
        private val JOINED_AT: LocalDateTime = LocalDateTime.of(2026, 8, 1, 9, 0)
    }
}
