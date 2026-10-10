package com.ogonggo.userapi.notification.fcm.presentation

import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.notification.fcm.business.FcmTestNotificationService
import com.ogonggo.userapi.notification.fcm.business.SendFcmTestNotificationCommand
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [FcmTestNotificationController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class FcmTestNotificationControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var fcmTestNotificationService: FcmTestNotificationService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `인증된 사용자가 FCM 테스트 알림을 요청하면 notification 적재 성공을 반환한다`() {
        // given
        Mockito.`when`(
            fcmTestNotificationService.send(
                USER_ID,
                SendFcmTestNotificationCommand(
                    title = "테스트 제목",
                    body = "테스트 본문",
                    data = emptyMap(),
                ),
            ),
        ).thenReturn("fcm-test:$USER_ID:test-key")

        // when
        mockMvc.perform(
            post("/api/v1/users/me/notifications/fcm/test")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"테스트 제목","body":"테스트 본문"}"""),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.deduplicationKey").value("fcm-test:$USER_ID:test-key"))

        Mockito.verify(fcmTestNotificationService).send(
            USER_ID,
            SendFcmTestNotificationCommand(
                title = "테스트 제목",
                body = "테스트 본문",
                data = emptyMap(),
            ),
        )
    }

    @Test
    fun `인증되지 않은 사용자는 FCM 테스트 알림을 요청할 수 없다`() {
        // when
        mockMvc.perform(
            post("/api/v1/users/me/notifications/fcm/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"테스트 제목","body":"테스트 본문"}"""),
        )
            // then
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        Mockito.verifyNoInteractions(fcmTestNotificationService)
    }

    @Test
    fun `제목이 비어 있으면 FCM 테스트 알림을 요청할 수 없다`() {
        // when
        mockMvc.perform(
            post("/api/v1/users/me/notifications/fcm/test")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"   ","body":"테스트 본문"}"""),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(fcmTestNotificationService)
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    companion object {
        private const val USER_ID = 17L
    }
}
