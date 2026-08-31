package com.ogonggo.userapi.auth.presentation

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.userapi.auth.business.AuthTokens
import com.ogonggo.userapi.auth.business.CompanyAuthService
import com.ogonggo.userapi.auth.business.CompanySignInCommand
import com.ogonggo.userapi.auth.business.CompanySignUpCommand
import com.ogonggo.userapi.auth.error.AuthErrorCode
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [CompanyAuthController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class CompanyAuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var companyAuthService: CompanyAuthService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `기업 회원가입은 오공고 세션 없이 호출하고 201로 응답한다`() {
        Mockito.`when`(companyAuthService.signUp(SIGN_UP_COMMAND)).thenReturn(TOKENS)

        mockMvc.perform(
            post("/api/v1/auth/company/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGN_UP_BODY),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.accessToken").value("og-access"))
            .andExpect(jsonPath("$.data.refreshToken").value("og-refresh"))
    }

    @Test
    fun `이미 쓰는 이메일이면 409 계약으로 응답한다`() {
        Mockito.`when`(companyAuthService.signUp(SIGN_UP_COMMAND))
            .thenThrow(ConflictException(UserErrorCode.EMAIL_ALREADY_EXISTS))

        mockMvc.perform(
            post("/api/v1/auth/company/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGN_UP_BODY),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
    }

    @Test
    fun `이메일 형식과 비밀번호 길이를 검증한다`() {
        listOf(
            """{"email":"not-an-email","password":"password1234","organizationName":"렛츠커리어","managerName":"김담당"}"""
                to "email",
            """{"email":"company@example.com","password":"short","organizationName":"렛츠커리어","managerName":"김담당"}"""
                to "password",
        ).forEach { (body, field) ->
            mockMvc.perform(
                post("/api/v1/auth/company/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(startsWith("[$field] ")))
        }

        Mockito.verifyNoInteractions(companyAuthService)
    }

    @Test
    fun `기업 로그인은 200으로 응답한다`() {
        Mockito.`when`(companyAuthService.signIn(SIGN_IN_COMMAND)).thenReturn(TOKENS)

        mockMvc.perform(
            post("/api/v1/auth/company/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGN_IN_BODY),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.accessToken").value("og-access"))
    }

    @Test
    fun `자격증명이 맞지 않으면 401 계약으로 응답한다`() {
        Mockito.`when`(companyAuthService.signIn(SIGN_IN_COMMAND))
            .thenThrow(UnauthorizedException(AuthErrorCode.INVALID_COMPANY_CREDENTIALS))

        mockMvc.perform(
            post("/api/v1/auth/company/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGN_IN_BODY),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_COMPANY_CREDENTIALS"))
    }

    companion object {
        private val TOKENS = AuthTokens(accessToken = "og-access", refreshToken = "og-refresh")
        private const val SIGN_UP_BODY =
            """{"email":"company@example.com","password":"password1234","organizationName":"렛츠커리어","managerName":"김담당"}"""
        private const val SIGN_IN_BODY =
            """{"email":"company@example.com","password":"password1234"}"""
        private val SIGN_UP_COMMAND = CompanySignUpCommand(
            email = "company@example.com",
            password = "password1234",
            organizationName = "렛츠커리어",
            managerName = "김담당",
        )
        private val SIGN_IN_COMMAND = CompanySignInCommand(
            email = "company@example.com",
            password = "password1234",
        )
    }
}
