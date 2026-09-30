package com.ogonggo.userapi.user.presentation

import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.user.business.PasswordChangeCommand
import com.ogonggo.userapi.user.business.UserAccountService
import com.ogonggo.userapi.user.business.UserPasswordService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UserAccountSettingController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserAccountSettingControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userAccountService: UserAccountService

    @MockBean
    private lateinit var userPasswordService: UserPasswordService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `수신 이메일을 받아 바꾸고 빼면 비운다`() {
        mockMvc.perform(
            put(NOTIFICATION_EMAIL_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationEmail":"today@example.com"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)

        mockMvc.perform(
            put(NOTIFICATION_EMAIL_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isOk)

        Mockito.verify(userAccountService).changeMyNotificationEmail(USER_ID, "today@example.com")
        Mockito.verify(userAccountService).changeMyNotificationEmail(USER_ID, null)
    }

    @Test
    fun `수신 이메일 형식이 아니거나 공백이면 400으로 막는다`() {
        listOf("""{"notificationEmail":"today"}""", """{"notificationEmail":""}""").forEach { body ->
            mockMvc.perform(
                put(NOTIFICATION_EMAIL_PATH).with(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(userAccountService)
    }

    @Test
    fun `업로드한 이미지 식별자를 받아 프로필 이미지를 바꾼다`() {
        mockMvc.perform(
            put(PROFILE_IMAGE_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"imageId":"image-id"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)

        Mockito.verify(userAccountService).replaceMyProfileImage(USER_ID, "image-id")
    }

    @Test
    fun `이미지 식별자가 없거나 비었으면 400으로 막는다`() {
        listOf("{}", """{"imageId":" "}""").forEach { body ->
            mockMvc.perform(
                put(PROFILE_IMAGE_PATH).with(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(userAccountService)
    }

    @Test
    fun `쓸 수 없는 이미지면 400 IMAGE_ASSET_NOT_AVAILABLE로 응답한다`() {
        Mockito.`when`(userAccountService.replaceMyProfileImage(USER_ID, "other-image"))
            .thenThrow(InvalidValueException(ImageUploadErrorCode.IMAGE_ASSET_NOT_AVAILABLE))

        mockMvc.perform(
            put(PROFILE_IMAGE_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"imageId":"other-image"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("IMAGE_ASSET_NOT_AVAILABLE"))
    }

    @Test
    fun `프로필 이미지를 지운다`() {
        mockMvc.perform(delete(PROFILE_IMAGE_PATH).with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)

        Mockito.verify(userAccountService).deleteMyProfileImage(USER_ID)
    }

    @Test
    fun `기존 비밀번호와 새 비밀번호를 받아 바꾼다`() {
        mockMvc.perform(
            patch(PASSWORD_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword":"old-password!","newPassword":"new-password!"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)

        Mockito.verify(userPasswordService).changeMyPassword(
            USER_ID,
            PasswordChangeCommand(currentPassword = "old-password!", newPassword = "new-password!"),
        )
    }

    @Test
    fun `비밀번호가 비었거나 새 비밀번호가 8자 미만이면 400으로 막는다`() {
        listOf(
            """{"currentPassword":"","newPassword":"new-password!"}""",
            """{"currentPassword":"old-password!","newPassword":"short!"}""",
            """{"currentPassword":"old-password!"}""",
        ).forEach { body ->
            mockMvc.perform(
                patch(PASSWORD_PATH).with(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(userPasswordService)
    }

    @Test
    fun `기존 비밀번호가 틀리면 400 CURRENT_PASSWORD_MISMATCH로 응답한다`() {
        Mockito.`when`(
            userPasswordService.changeMyPassword(
                USER_ID,
                PasswordChangeCommand(currentPassword = "wrong-password!", newPassword = "new-password!"),
            ),
        ).thenThrow(InvalidValueException(UserErrorCode.CURRENT_PASSWORD_MISMATCH))

        mockMvc.perform(
            patch(PASSWORD_PATH).with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword":"wrong-password!","newPassword":"new-password!"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_MISMATCH"))
    }

    @Test
    fun `인증이 없으면 401로 응답한다`() {
        mockMvc.perform(
            put(NOTIFICATION_EMAIL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationEmail":"today@example.com"}"""),
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            patch(PASSWORD_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword":"old-password!","newPassword":"new-password!"}"""),
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(delete(PROFILE_IMAGE_PATH))
            .andExpect(status().isUnauthorized)
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    companion object {
        private const val NOTIFICATION_EMAIL_PATH = "/api/v1/users/me/notification-email"
        private const val PASSWORD_PATH = "/api/v1/users/me/password"
        private const val PROFILE_IMAGE_PATH = "/api/v1/users/me/profile-image"
        private const val USER_ID = 17L
    }
}
