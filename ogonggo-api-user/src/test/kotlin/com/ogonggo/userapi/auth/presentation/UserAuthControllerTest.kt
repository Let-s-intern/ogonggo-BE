package com.ogonggo.userapi.auth.presentation

import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.userapi.auth.business.AuthTokens
import com.ogonggo.userapi.auth.business.SignInResult
import com.ogonggo.userapi.auth.business.UserAuthService
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.auth.error.AuthErrorCode
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UserAuthController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserAuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userAuthService: UserAuthService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `렛츠커리어 토큰 교환은 오공고 세션 없이 호출한다`() {
        Mockito.`when`(userAuthService.signInWithLetsCareer("lc-access-token")).thenReturn(
            SignInResult(
                tokens = AuthTokens(accessToken = "og-access", refreshToken = "og-refresh"),
                isNewUser = true,
            ),
        )

        mockMvc.perform(
            post("/api/v1/auth/letscareer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"letsCareerAccessToken":"lc-access-token"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.accessToken").value("og-access"))
            .andExpect(jsonPath("$.data.refreshToken").value("og-refresh"))
            .andExpect(jsonPath("$.data.isNewUser").value(true))
    }

    @Test
    fun `렛츠커리어 토큰이 유효하지 않으면 401 계약으로 응답한다`() {
        Mockito.`when`(userAuthService.signInWithLetsCareer("lc-access-token"))
            .thenThrow(UnauthorizedException(AuthErrorCode.INVALID_LETSCAREER_TOKEN))

        mockMvc.perform(
            post("/api/v1/auth/letscareer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"letsCareerAccessToken":"lc-access-token"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_LETSCAREER_TOKEN"))
    }

    @Test
    fun `공백인 인증 요청 필드는 필드명이 포함된 400 계약으로 응답한다`() {
        listOf(
            "/api/v1/auth/letscareer" to "letsCareerAccessToken",
            "/api/v1/auth/token" to "refreshToken",
        ).forEach { (path, field) ->
            mockMvc.perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"$field":"   "}"""),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(startsWith("[$field] ")))
        }

        Mockito.verifyNoInteractions(userAuthService)
    }

    @Test
    fun `필수 인증 필드가 누락되면 기본 400 계약으로 응답한다`() {
        mockMvc.perform(
            post("/api/v1/auth/letscareer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))

        Mockito.verifyNoInteractions(userAuthService)
    }

    @Test
    fun `읽을 수 없는 JSON이면 기본 400 계약으로 응답한다`() {
        mockMvc.perform(
            post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"token"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))

        Mockito.verifyNoInteractions(userAuthService)
    }

    @Test
    fun `오공고 액세스 토큰으로 로그아웃한다`() {
        Mockito.`when`(ogonggoTokenProvider.parseAccessToken("og-access")).thenReturn(17L)

        mockMvc.perform(
            post("/api/v1/auth/signout").header(HttpHeaders.AUTHORIZATION, "Bearer og-access"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(userAuthService).signOut(17L)
    }

    @Test
    fun `토큰 없이 로그아웃하면 401로 응답한다`() {
        mockMvc.perform(post("/api/v1/auth/signout"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `유효하지 않은 액세스 토큰은 인증되지 않은 요청으로 처리한다`() {
        Mockito.`when`(ogonggoTokenProvider.parseAccessToken("broken"))
            .thenThrow(UnauthorizedException(AuthErrorCode.INVALID_TOKEN))

        mockMvc.perform(
            post("/api/v1/auth/signout").header(HttpHeaders.AUTHORIZATION, "Bearer broken"),
        )
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(userAuthService)
    }
}
