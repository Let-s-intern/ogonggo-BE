package com.ogonggo.userapi.notification.fcm.presentation

import com.ogonggo.core.userfcmtoken.implement.dto.UserFcmTokenRegisterDto
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType
import java.util.UUID

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

        Mockito.verify(fcmTokenService).replace(USER_ID, UserFcmTokenRegisterDto("fcm-token-1", null))
    }

    @Test
    fun `설치 UUID와 최대 길이 토큰을 받아 저장한다`() {
        // given
        val token = "a".repeat(4096)
        val deviceId = "A03A7396-907A-4E7F-A127-0CF5A80565CA"

        // when, then
        mockMvc.perform(
            put("/api/v1/users/me/fcm-token").with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"$token","deviceId":"$deviceId"}"""),
        ).andExpect(status().isOk).andExpect(jsonPath("$.data.token").value(token))
        Mockito.verify(fcmTokenService).replace(USER_ID, UserFcmTokenRegisterDto(token, UUID.fromString(deviceId)))
    }

    @Test
    fun `잘못된 설치 UUID와 길이 초과 및 누락 토큰은 400이다`() {
        // given
        val invalidBodies = listOf(
            """{"token":"token","deviceId":"not-a-uuid"}""",
            """{"token":"token","deviceId":"1-1-1-1-1"}""",
            """{"token":"token","deviceId":""}""",
            """{"token":"${"a".repeat(4097)}"}""",
            """{"deviceId":null}""",
            """{"token":null}""",
        )
        // when, then
        invalidBodies.forEach { body ->
            mockMvc.perform(
                put("/api/v1/users/me/fcm-token").with(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON).content(body),
            ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }
        Mockito.verifyNoInteractions(fcmTokenService)
    }

    @Test
    fun `토큰 해제는 200과 null 데이터를 반환한다`() {
        // when, then
        repeat(2) {
            mockMvc.perform(
                delete("/api/v1/users/me/fcm-token").with(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON).content("""{"token":"token"}"""),
            ).andExpect(status().isOk).andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
        Mockito.verify(fcmTokenService, Mockito.times(2)).delete(USER_ID, "token")
    }

    @Test
    fun `해제 토큰도 필수이고 최대 길이를 검증한다`() {
        // given
        val bodies = listOf("{}", """{"token":" "}""", """{"token":"${"a".repeat(4097)}"}""")
        // when, then
        bodies.forEach { body ->
            mockMvc.perform(
                delete("/api/v1/users/me/fcm-token").with(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON).content(body),
            ).andExpect(status().isBadRequest)
        }
        Mockito.verifyNoInteractions(fcmTokenService)
    }

    @Test
    fun `인증 없이 토큰 조회 및 해제를 할 수 없다`() {
        // when, then
        mockMvc.perform(get("/api/v1/users/me/fcm-token")).andExpect(status().isUnauthorized)
        mockMvc.perform(
            delete("/api/v1/users/me/fcm-token").contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"token"}"""),
        ).andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(fcmTokenService)
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
