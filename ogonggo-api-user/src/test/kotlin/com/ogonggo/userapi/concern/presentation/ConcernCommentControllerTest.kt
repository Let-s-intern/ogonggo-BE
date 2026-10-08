package com.ogonggo.userapi.concern.presentation

import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.concern.business.ConcernAuthorResult
import com.ogonggo.userapi.concern.business.ConcernCommentPageResult
import com.ogonggo.userapi.concern.business.ConcernCommentReplyPageResult
import com.ogonggo.userapi.concern.business.ConcernCommentResult
import com.ogonggo.userapi.concern.business.ConcernCommentRootResult
import com.ogonggo.userapi.concern.business.ConcernCommentService
import com.ogonggo.userapi.concern.business.CreateConcernCommentCommand
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [ConcernCommentController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class ConcernCommentControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var concernCommentService: ConcernCommentService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `비로그인 사용자도 답변과 답글 미리보기를 조회한다`() {
        // given
        Mockito.`when`(concernCommentService.readComments(null, CONCERN_ID, 0, 10)).thenReturn(
            ConcernCommentPageResult(
                items = listOf(
                    ConcernCommentRootResult(
                        comment = commentResult(official = true),
                        replies = ConcernCommentReplyPageResult(emptyList(), page = 0, size = 5, totalElements = 0, totalPages = 0),
                    ),
                ),
                page = 0,
                size = 10,
                totalElements = 1,
                totalPages = 1,
            ),
        )

        // when
        mockMvc.perform(get("/api/v1/concerns/$CONCERN_ID/comments"))
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].official").value(true))
            .andExpect(jsonPath("$.data.items[0].likeCount").value(51))
            .andExpect(jsonPath("$.data.items[0].replies.pageInfo.totalElements").value(0))
    }

    @Test
    fun `답글 미리보기보다 큰 페이지 크기는 400이다`() {
        // when
        mockMvc.perform(get("/api/v1/concerns/$CONCERN_ID/comments").param("size", "31"))
            // then
            .andExpect(status().isBadRequest)

        Mockito.verifyNoInteractions(concernCommentService)
    }

    @Test
    fun `로그인한 사용자가 답글을 작성하면 201과 식별자를 준다`() {
        // given
        Mockito.`when`(
            concernCommentService.create(
                USER_ID,
                CONCERN_ID,
                CreateConcernCommentCommand(parentId = COMMENT_ID, content = "감사합니다"),
            ),
        ).thenReturn(102L)

        // when
        mockMvc.perform(
            post("/api/v1/concerns/$CONCERN_ID/comments")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"감사합니다","parentId":$COMMENT_ID}"""),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(102))
    }

    @Test
    fun `좋아요를 누르고 취소한다`() {
        // when
        mockMvc.perform(put("/api/v1/concerns/$CONCERN_ID/comments/$COMMENT_ID/likes/me").with(authenticatedUser()))
            .andExpect(status().isOk)
        mockMvc.perform(delete("/api/v1/concerns/$CONCERN_ID/comments/$COMMENT_ID/likes/me").with(authenticatedUser()))
            .andExpect(status().isOk)

        // then
        Mockito.verify(concernCommentService).like(USER_ID, CONCERN_ID, COMMENT_ID)
        Mockito.verify(concernCommentService).unlike(USER_ID, CONCERN_ID, COMMENT_ID)
    }

    @Test
    fun `비로그인 사용자는 좋아요를 누를 수 없다`() {
        // when
        mockMvc.perform(put("/api/v1/concerns/$CONCERN_ID/comments/$COMMENT_ID/likes/me"))
            // then
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(concernCommentService)
    }

    private fun commentResult(official: Boolean) = ConcernCommentResult(
        id = COMMENT_ID,
        parentId = null,
        author = ConcernAuthorResult(nickname = "렛츠커리어 매니저 쥬디", profileImageUrl = null),
        official = official,
        content = "전환율은 공개된 수치가 없어요.",
        deleted = false,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
        mine = false,
        likeCount = 51,
        liked = false,
    )

    private fun authenticatedUser() = authentication(UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()))

    companion object {
        private const val USER_ID = 17L
        private const val CONCERN_ID = 5L
        private const val COMMENT_ID = 101L
        private val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 10, 2, 10, 0)
    }
}
