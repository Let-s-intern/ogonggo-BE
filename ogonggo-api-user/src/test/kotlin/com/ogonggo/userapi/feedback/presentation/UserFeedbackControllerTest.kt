package com.ogonggo.userapi.feedback.presentation

import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.feedback.business.CreateFeedbackCommand
import com.ogonggo.userapi.feedback.business.UserFeedbackService
import org.hamcrest.Matchers.startsWith
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

@WebMvcTest(controllers = [UserFeedbackController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserFeedbackControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userFeedbackService: UserFeedbackService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `로그인 없이 개선 의견을 남기면 201과 식별자를 준다`() {
        // given
        val command = CreateFeedbackCommand(satisfaction = "달력이 편해요", improvement = "알림이 필요해요")
        Mockito.`when`(userFeedbackService.createFeedback(null, command)).thenReturn(FEEDBACK_ID)

        // when & then
        mockMvc.perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"satisfaction":"달력이 편해요","improvement":"알림이 필요해요"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(FEEDBACK_ID))
    }

    @Test
    fun `로그인하면 작성자를 넘기고 공백만 있는 문항은 비운 것으로 본다`() {
        // given
        val command = CreateFeedbackCommand(satisfaction = null, improvement = "알림이 필요해요")
        Mockito.`when`(userFeedbackService.createFeedback(USER_ID, command)).thenReturn(FEEDBACK_ID)

        // when & then
        mockMvc.perform(
            post(PATH)
                .with(authentication(UsernamePasswordAuthenticationToken(USER_ID, null, emptyList())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"satisfaction":"  ","improvement":"알림이 필요해요"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(FEEDBACK_ID))
    }

    @Test
    fun `두 문항이 모두 비었거나 1000자를 넘으면 400으로 응답한다`() {
        listOf(
            """{}""" to "satisfaction",
            """{"satisfaction":" ","improvement":""}""" to "satisfaction",
            """{"improvement":"${"가".repeat(1001)}"}""" to "improvement",
        ).forEach { (body, field) ->
            mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(startsWith("[$field] ")))
        }

        Mockito.verifyNoInteractions(userFeedbackService)
    }

    private companion object {
        const val PATH = "/api/v1/feedbacks"
        const val USER_ID = 17L
        const val FEEDBACK_ID = 5L
    }
}
