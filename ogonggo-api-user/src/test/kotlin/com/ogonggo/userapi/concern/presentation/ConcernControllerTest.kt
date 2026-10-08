package com.ogonggo.userapi.concern.presentation

import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernSortType
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.concern.business.ConcernAuthorResult
import com.ogonggo.userapi.concern.business.ConcernListQuery
import com.ogonggo.userapi.concern.business.ConcernPageResult
import com.ogonggo.userapi.concern.business.ConcernService
import com.ogonggo.userapi.concern.business.ConcernSummaryResult
import com.ogonggo.userapi.concern.business.SaveConcernCommand
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [ConcernController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class ConcernControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var concernService: ConcernService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `비로그인 사용자도 카테고리와 정렬을 골라 목록을 조회한다`() {
        // given
        val query = ConcernListQuery(ConcernCategory.SIDE_EXPERIENCE, ConcernSortType.VIEW_COUNT, page = 0, size = 3)
        Mockito.`when`(concernService.readConcerns(query)).thenReturn(
            ConcernPageResult(items = listOf(summary()), page = 0, size = 3, totalElements = 8, totalPages = 3),
        )

        // when
        mockMvc.perform(
            get("/api/v1/concerns")
                .param("category", "SIDE_EXPERIENCE")
                .param("sort", "VIEW_COUNT")
                .param("size", "3"),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(CONCERN_ID))
            .andExpect(jsonPath("$.data.items[0].hasOfficialComment").value(true))
            .andExpect(jsonPath("$.data.items[0].author.nickname").value("슬픈 어피치"))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(8))
    }

    @Test
    fun `정의되지 않은 카테고리는 400이다`() {
        // when
        mockMvc.perform(get("/api/v1/concerns").param("category", "UNKNOWN"))
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(concernService)
    }

    @Test
    fun `로그인한 사용자가 고민글을 작성하면 201과 식별자를 준다`() {
        // given
        val command = SaveConcernCommand(ConcernCategory.JOB_CAREER, "정규직 전환율 높은 편일까요?", "궁금합니다.")
        Mockito.`when`(concernService.create(USER_ID, command)).thenReturn(CONCERN_ID)

        // when
        mockMvc.perform(
            post("/api/v1/concerns")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"category":"JOB_CAREER","title":"정규직 전환율 높은 편일까요?","content":"궁금합니다."}"""),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").value(CONCERN_ID))
    }

    @Test
    fun `제목이 비면 400이다`() {
        // when
        mockMvc.perform(
            post("/api/v1/concerns")
                .with(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"category":"ETC","title":"  ","content":"본문"}"""),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(concernService)
    }

    @Test
    fun `비로그인 사용자는 고민글을 작성할 수 없다`() {
        // when
        mockMvc.perform(
            post("/api/v1/concerns")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"category":"ETC","title":"제목","content":"본문"}"""),
        )
            // then
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(concernService)
    }

    private fun summary() = ConcernSummaryResult(
        id = CONCERN_ID,
        category = ConcernCategory.SIDE_EXPERIENCE,
        title = "사이드 프로젝트 팀원은 주로 어디서 구하나요?",
        content = "사프 해보고 싶은데요.",
        author = ConcernAuthorResult(nickname = "슬픈 어피치", profileImageUrl = null),
        createdAt = LocalDateTime.of(2026, 10, 3, 10, 0),
        viewCount = 1980,
        commentCount = 1,
        hasOfficialComment = true,
    )

    private fun authenticatedUser() = authentication(UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()))

    companion object {
        private const val USER_ID = 17L
        private const val CONCERN_ID = 5L
    }
}
