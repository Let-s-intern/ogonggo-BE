package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.community.business.CreateRecruitmentPostCommentCommand
import com.ogonggo.userapi.community.business.RecruitmentPostCommentService
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
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

@WebMvcTest(controllers = [RecruitmentPostCommentController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class RecruitmentPostCommentControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var recruitmentPostCommentService: RecruitmentPostCommentService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `인증된 사용자가 모집글에 댓글을 작성하면 201과 식별자를 반환한다`() {
        // given
        Mockito.`when`(
            recruitmentPostCommentService.create(
                USER_ID,
                POST_ID,
                command(),
            ),
        ).thenReturn(COMMENT_ID)

        // when
        mockMvc.perform(
            post("/api/v1/recruitment-posts/$POST_ID/comments")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"참여하고 싶습니다."}"""),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.id").value(COMMENT_ID))

        Mockito.verify(recruitmentPostCommentService).create(USER_ID, POST_ID, command())
    }

    @Test
    fun `인증되지 않은 사용자는 댓글을 작성할 수 없다`() {
        // given
        // when
        mockMvc.perform(
            post("/api/v1/recruitment-posts/$POST_ID/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"참여하고 싶습니다."}"""),
        )
            // then
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        Mockito.verifyNoInteractions(recruitmentPostCommentService)
    }

    @Test
    fun `댓글 내용이 공백이면 400을 반환한다`() {
        // given
        // when
        mockMvc.perform(
            post("/api/v1/recruitment-posts/$POST_ID/comments")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"   "}"""),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(recruitmentPostCommentService)
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    private fun command() = CreateRecruitmentPostCommentCommand(
        parentId = null,
        content = "참여하고 싶습니다.",
    )

    companion object {
        private const val USER_ID = 17L
        private const val POST_ID = 12L
        private const val COMMENT_ID = 101L
    }
}
