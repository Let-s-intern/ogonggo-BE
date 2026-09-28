package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPostBookmarkSearchCondition
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.error.RecruitmentPostApplicationErrorCode
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.community.business.RecruitmentPostBookmarkService
import com.ogonggo.userapi.community.business.RecruitmentPostBookmarkPageResult
import com.ogonggo.userapi.community.business.RecruitmentPostAuthorResult
import com.ogonggo.userapi.community.business.RecruitmentPostSummary
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [RecruitmentPostBookmarkController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class RecruitmentPostBookmarkControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var bookmarkService: RecruitmentPostBookmarkService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `내 모집글 북마크 목록을 페이지로 조회한다`() {
        Mockito.`when`(bookmarkService.getBookmarks(USER_ID, 0, 10)).thenReturn(bookmarkPage())

        mockMvc.perform(
            get("/api/v1/recruitment-post-bookmarks")
                .with(authenticatedUser())
                .param("page", "1"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.items[0].id").value(POST_ID))
            .andExpect(jsonPath("$.data.items[0].bookmarked").value(true))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
            .andExpect(jsonPath("$.data.pageInfo.totalPages").value(1))
    }

    @Test
    fun `모집글 북마크를 등록하고 해제한다`() {
        mockMvc.perform(put("/api/v1/recruitment-posts/{postId}/bookmarks/me", POST_ID).with(authenticatedUser()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))

        mockMvc.perform(delete("/api/v1/recruitment-posts/{postId}/bookmarks/me", POST_ID).with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))

        Mockito.verify(bookmarkService).addBookmark(USER_ID, POST_ID)
        Mockito.verify(bookmarkService).deleteBookmark(USER_ID, POST_ID)
    }

    @Test
    fun `중복 모집글 북마크는 409로 응답한다`() {
        Mockito.doThrow(ConflictException(RecruitmentPostErrorCode.RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS))
            .`when`(bookmarkService).addBookmark(USER_ID, POST_ID)

        mockMvc.perform(put("/api/v1/recruitment-posts/{postId}/bookmarks/me", POST_ID).with(authenticatedUser()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS"))
    }

    @Test
    fun `인증이 없거나 모집글 식별자가 잘못되면 표준 오류로 응답한다`() {
        mockMvc.perform(get("/api/v1/recruitment-post-bookmarks"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc.perform(put("/api/v1/recruitment-posts/0/bookmarks/me").with(authenticatedUser()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `없는 모집글의 북마크 등록은 404로 응답한다`() {
        Mockito.doThrow(EntityNotFoundException(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND))
            .`when`(bookmarkService).addBookmark(USER_ID, POST_ID)

        mockMvc.perform(put("/api/v1/recruitment-posts/{postId}/bookmarks/me", POST_ID).with(authenticatedUser()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECRUITMENT_POST_NOT_FOUND"))
    }

    @Test
    fun `북마크 목록의 모집 상태와 유형과 검색어는 조회 조건으로 전달된다`() {
        // given
        val condition = RecruitmentPostBookmarkSearchCondition(
            recruitmentStatus = RecruitmentStatus.RECRUITING,
            recruitmentType = RecruitmentType.STUDY,
            keyword = "코틀린",
            sortType = BookmarkSortType.RECENTLY_SAVED,
        )
        Mockito.`when`(bookmarkService.getBookmarks(USER_ID, 0, 10, condition)).thenReturn(bookmarkPage())

        // when
        mockMvc.perform(
            get("/api/v1/recruitment-post-bookmarks")
                .param("recruitmentStatus", "RECRUITING")
                .param("recruitmentType", "STUDY")
                .param("keyword", " 코틀린 ")
                .with(authenticatedUser()),
        ).andExpect(status().isOk)

        // then
        Mockito.verify(bookmarkService).getBookmarks(USER_ID, 0, 10, condition)
    }

    @Test
    fun `스크랩과 지원 준비 중 사이를 옮긴다`() {
        mockMvc.perform(post("/api/v1/recruitment-post-bookmarks/{postId}/prepare", POST_ID).with(authenticatedUser()))
            .andExpect(status().isOk)
        mockMvc.perform(
            post("/api/v1/recruitment-post-bookmarks/{postId}/cancel-preparation", POST_ID).with(authenticatedUser()),
        ).andExpect(status().isOk)

        Mockito.verify(bookmarkService).prepare(USER_ID, POST_ID)
        Mockito.verify(bookmarkService).cancelPreparation(USER_ID, POST_ID)
    }

    @Test
    fun `지원 완료 이후 단계에서 옮기면 409로 응답한다`() {
        Mockito.doThrow(
            ConflictException(RecruitmentPostApplicationErrorCode.INVALID_RECRUITMENT_APPLICATION_STATUS_TRANSITION),
        ).`when`(bookmarkService).prepare(USER_ID, POST_ID)

        mockMvc.perform(post("/api/v1/recruitment-post-bookmarks/{postId}/prepare", POST_ID).with(authenticatedUser()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INVALID_RECRUITMENT_APPLICATION_STATUS_TRANSITION"))
    }

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    private fun bookmarkPage() = RecruitmentPostBookmarkPageResult(
        items = listOf(
            RecruitmentPostSummary(
                id = POST_ID,
                author = RecruitmentPostAuthorResult(
                    userId = USER_ID,
                    nickname = "홍길동",
                    profileImageUrl = "https://cdn.example.com/17.png",
                ),
                title = "Kotlin 사이드 프로젝트 팀원 모집",
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                progressMethod = ProgressMethod.ONLINE,
                recruitmentStatus = RecruitmentStatus.RECRUITING,
                capacity = 4,
                activityDurationMonths = 3,
                technologyStacks = listOf("Kotlin"),
                recruitmentStartDate = LocalDate.of(2026, 9, 1),
                recruitmentEndDate = LocalDate.of(2026, 9, 30),
                bookmarked = true,
            ),
        ),
        page = 0,
        size = 10,
        totalElements = 1,
        totalPages = 1,
    )

    companion object {
        private const val USER_ID = 17L
        private const val POST_ID = 12L
    }
}
