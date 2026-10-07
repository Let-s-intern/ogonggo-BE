package com.ogonggo.userapi.notification.fcm.presentation

import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.notification.fcm.business.FcmTokenService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType

@WebMvcTest(controllers = [FcmTokenController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class FcmTokenControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var fcmTokenService: FcmTokenService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인한 사용자의 FCM 토큰을 저장한다`() {
        mockMvc.perform(
            put("/api/v1/users/me/fcm-token")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"fcm-token-1"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.token").value("fcm-token-1"))

        Mockito.verify(fcmTokenService).replace(USER_ID, "fcm-token-1")
    }

    @Test
    fun `로그인한 사용자의 FCM 토큰을 조회한다`() {
        Mockito.`when`(fcmTokenService.get(USER_ID)).thenReturn("fcm-token-1")

        mockMvc.perform(get("/api/v1/users/me/fcm-token").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.token").value("fcm-token-1"))
    }

    @Test
    fun `빈 FCM 토큰은 400으로 거절한다`() {
        mockMvc.perform(
            put("/api/v1/users/me/fcm-token")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"   "}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(fcmTokenService)
    }

    @Test
    fun `인증 없이 FCM 토큰을 저장할 수 없다`() {
        mockMvc.perform(
            put("/api/v1/users/me/fcm-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"fcm-token-1"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    companion object {
        private const val USER_ID = 17L
    }
}
