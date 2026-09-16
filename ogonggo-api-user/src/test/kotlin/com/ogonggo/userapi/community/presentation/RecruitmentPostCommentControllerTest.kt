package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.community.business.CreateRecruitmentPostCommentCommand
import com.ogonggo.userapi.community.business.RecruitmentPostCommentAuthorResult
import com.ogonggo.userapi.community.business.RecruitmentPostCommentPageResult
import com.ogonggo.userapi.community.business.RecruitmentPostCommentReplyPageResult
import com.ogonggo.userapi.community.business.RecruitmentPostCommentResult
import com.ogonggo.userapi.community.business.RecruitmentPostCommentRootResult
import com.ogonggo.userapi.community.business.RecruitmentPostCommentService
import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostCommentReportRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [RecruitmentPostCommentController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class RecruitmentPostCommentControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `비로그인 사용자가 부모 댓글을 페이지로 조회하면 대댓글 미리보기도 페이지 정보와 함께 반환한다`() {
        // given
        Mockito.`when`(
            recruitmentPostCommentService.readComments(null, POST_ID, 0, 10),
        ).thenReturn(
            RecruitmentPostCommentPageResult(
                items = listOf(
                    RecruitmentPostCommentRootResult(
                        comment = commentResult(),
                        replies = RecruitmentPostCommentReplyPageResult(
                            items = listOf(commentResult(parentId = COMMENT_ID)),
                            page = 0,
                            size = 5,
                            totalElements = 6,
                            totalPages = 2,
                        ),
                    ),
                ),
                page = 0,
                size = 10,
                totalElements = 1,
                totalPages = 1,
            ),
        )

        // when
        mockMvc.perform(
            get("/api/v1/recruitment-posts/$POST_ID/comments")
                .param("page", "1")
                .param("size", "10"),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(COMMENT_ID))
            .andExpect(jsonPath("$.data.items[0].replies.items[0].parentId").value(COMMENT_ID))
            .andExpect(jsonPath("$.data.items[0].replies.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.items[0].replies.pageInfo.totalElements").value(6))
            .andExpect(jsonPath("$.data.items[0].replies.pageInfo.totalPages").value(2))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))

        Mockito.verify(recruitmentPostCommentService).readComments(null, POST_ID, 0, 10)
    }

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
    fun `인증된 사용자가 댓글을 삭제하면 200을 반환한다`() {
        // given
        // when
        mockMvc.perform(
            delete("/api/v1/recruitment-posts/$POST_ID/comments/$COMMENT_ID")
                .with(authenticatedUser()),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))

        Mockito.verify(recruitmentPostCommentService).delete(USER_ID, POST_ID, COMMENT_ID)
    }

    @Test
    fun `인증되지 않은 사용자는 댓글을 삭제할 수 없다`() {
        // given
        // when
        mockMvc.perform(
            delete("/api/v1/recruitment-posts/$POST_ID/comments/$COMMENT_ID"),
        )
            // then
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        Mockito.verifyNoInteractions(recruitmentPostCommentService)
    }

    @Test
    fun `인증된 사용자가 댓글을 사유 없이 신고하면 201을 반환한다`() {
        mockMvc.perform(
            post("/api/v1/recruitment-posts/$POST_ID/comments/$COMMENT_ID/reports")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))

        Mockito.verify(recruitmentPostCommentService).report(USER_ID, POST_ID, COMMENT_ID, null)
    }

    @Test
    fun `인증되지 않은 사용자는 댓글을 신고할 수 없다`() {
        mockMvc.perform(
            post("/api/v1/recruitment-posts/$POST_ID/comments/$COMMENT_ID/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
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

    private fun commentResult(parentId: Long? = null) = RecruitmentPostCommentResult(
        id = if (parentId == null) COMMENT_ID else 102L,
        parentId = parentId,
        author = RecruitmentPostCommentAuthorResult(
            userId = USER_ID,
            nickname = "닉네임",
            profileImageUrl = null,
        ),
        content = "댓글 내용",
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
        mine = false,
    )

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
        private val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 9, 12, 10, 0)
    }
}
