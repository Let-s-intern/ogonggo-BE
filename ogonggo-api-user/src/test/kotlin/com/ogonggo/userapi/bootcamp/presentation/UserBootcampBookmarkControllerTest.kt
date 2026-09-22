package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bootcamp.domain.BootcampApplicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampBookmarkSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.bootcamp.business.UserBootcampBookmarkService
import com.ogonggo.userapi.bootcamp.business.UserBootcampPageResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampSummary
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [UserBootcampBookmarkController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserBootcampBookmarkControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userBootcampBookmarkService: UserBootcampBookmarkService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `내 북마크 목록을 1 기반 페이지로 조회한다`() {
        Mockito.`when`(userBootcampBookmarkService.getBookmarks(USER_ID, BootcampSearchCondition.NONE, 0, 10))
            .thenReturn(bookmarkPage())

        mockMvc.perform(get("/api/v1/bootcamp-bookmarks").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(BOOTCAMP_ID))
            .andExpect(jsonPath("$.data.items[0].bookmarked").value(true))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
    }

    @Test
    fun `내 북마크 목록의 필터와 검색어는 조회 조건으로 전달된다`() {
        // given
        val condition = BootcampSearchCondition(
            tuitionType = TuitionType.FREE,
            status = BootcampStatus.RECRUITING,
            keyword = "백엔드",
        )
        Mockito.`when`(userBootcampBookmarkService.getBookmarks(USER_ID, condition, 0, 10))
            .thenReturn(bookmarkPage())

        // when
        mockMvc.perform(
            get("/api/v1/bootcamp-bookmarks")
                .param("tuitionType", "FREE")
                .param("status", "RECRUITING")
                .param("keyword", "백엔드")
                .with(authenticatedUser()),
        ).andExpect(status().isOk)

        // then
        Mockito.verify(userBootcampBookmarkService).getBookmarks(USER_ID, condition, 0, 10)
    }

    @Test
    fun `내 북마크 목록에서 고를 수 없는 모집 상태는 파라미터명이 포함된 400을 반환한다`() {
        mockMvc.perform(get("/api/v1/bootcamp-bookmarks").param("status", "DRAFT").with(authenticatedUser()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value(startsWith("[status] ")))

        Mockito.verifyNoInteractions(userBootcampBookmarkService)
    }

    @Test
    fun `북마크를 등록하고 해제한다`() {
        mockMvc.perform(post("/api/v1/bootcamp-bookmarks/{bootcampId}", BOOTCAMP_ID).with(authenticatedUser()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))

        mockMvc.perform(delete("/api/v1/bootcamp-bookmarks/{bootcampId}", BOOTCAMP_ID).with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))

        Mockito.verify(userBootcampBookmarkService).addBookmark(USER_ID, BOOTCAMP_ID)
        Mockito.verify(userBootcampBookmarkService).deleteBookmark(USER_ID, BOOTCAMP_ID)
    }

    @Test
    fun `중복 북마크는 도메인 409 계약으로 응답한다`() {
        Mockito.doThrow(ConflictException(BootcampErrorCode.BOOTCAMP_BOOKMARK_ALREADY_EXISTS))
            .`when`(userBootcampBookmarkService).addBookmark(USER_ID, BOOTCAMP_ID)

        mockMvc.perform(post("/api/v1/bootcamp-bookmarks/{bootcampId}", BOOTCAMP_ID).with(authenticatedUser()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("BOOTCAMP_BOOKMARK_ALREADY_EXISTS"))
            .andExpect(jsonPath("$.message").value("이미 북마크한 부트캠프입니다."))
    }

    @Test
    fun `인증이 없거나 부트캠프 식별자가 잘못되면 표준 오류로 응답한다`() {
        mockMvc.perform(get("/api/v1/bootcamp-bookmarks"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc.perform(post("/api/v1/bootcamp-bookmarks/0").with(authenticatedUser()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `신청 단계를 고르면 그 단계만 조회하도록 전달된다`() {
        // given
        Mockito.`when`(
            userBootcampBookmarkService.getBookmarks(USER_ID, BootcampSearchCondition.NONE, 0, 10, BootcampBookmarkSearchCondition(applicationStatus = BootcampApplicationStatus.PREPARING)),
        ).thenReturn(bookmarkPage())

        // when
        mockMvc.perform(
            get("/api/v1/bootcamp-bookmarks").param("applicationStatus", "PREPARING").with(authenticatedUser()),
        ).andExpect(status().isOk)

        // then
        Mockito.verify(userBootcampBookmarkService).getBookmarks(USER_ID, BootcampSearchCondition.NONE, 0, 10, BootcampBookmarkSearchCondition(applicationStatus = BootcampApplicationStatus.PREPARING))
    }

    @Test
    fun `북마크를 요청한 단계로 옮긴다`() {
        mockMvc.perform(
            put("/api/v1/bootcamp-bookmarks/{id}/application-status", BOOTCAMP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"applicationStatus":"IN_PROGRESS"}""")
                .with(authenticatedUser()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))

        Mockito.verify(userBootcampBookmarkService).changeApplicationStatus(USER_ID, BOOTCAMP_ID, BootcampApplicationStatus.IN_PROGRESS)
    }

    @Test
    fun `단계가 없거나 정의되지 않은 값이면 400으로 응답한다`() {
        listOf("{}", """{"applicationStatus":"UNKNOWN"}""").forEach { body ->
            mockMvc.perform(
                put("/api/v1/bootcamp-bookmarks/{id}/application-status", BOOTCAMP_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .with(authenticatedUser()),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }

        Mockito.verifyNoInteractions(userBootcampBookmarkService)
    }

    @Test
    fun `북마크하지 않은 대상의 단계를 옮기면 404로 응답한다`() {
        Mockito.doThrow(EntityNotFoundException(BootcampErrorCode.BOOTCAMP_BOOKMARK_NOT_FOUND))
            .`when`(userBootcampBookmarkService).changeApplicationStatus(USER_ID, BOOTCAMP_ID, BootcampApplicationStatus.IN_PROGRESS)

        mockMvc.perform(
            put("/api/v1/bootcamp-bookmarks/{id}/application-status", BOOTCAMP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"applicationStatus":"IN_PROGRESS"}""")
                .with(authenticatedUser()),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("BOOTCAMP_BOOKMARK_NOT_FOUND"))
    }

    private fun bookmarkPage(): UserBootcampPageResult = UserBootcampPageResult(
        items = listOf(
            UserBootcampSummary(
                id = BOOTCAMP_ID,
                companyName = "오공고 교육사",
                title = "백엔드 부트캠프",
                programType = "개발",
                operationType = OperationType.ONLINE,
                recruitmentType = BootcampRecruitmentType.PERIOD,
                recruitmentStartAt = null,
                recruitmentEndAt = null,
                programStartDate = LocalDate.of(2026, 9, 1),
                programEndDate = LocalDate.of(2026, 12, 1),
                capacity = 30,
                tuitionType = TuitionType.FREE,
                tuitionAmount = 0,
                representativeImageUrl = "https://example.com/image.png",
                shortDescription = "백엔드 개발자로 성장하는 12주",
                status = BootcampStatus.RECRUITING,
                closedAt = null,
                bookmarked = true,
                viewCount = 12,
                bookmarkCount = 3,
                commentCount = 0,
            ),
        ),
        page = 0,
        size = 10,
        totalElements = 1,
        totalPages = 1,
        hasNext = false,
    )

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    companion object {
        private const val USER_ID = 17L
        private const val BOOTCAMP_ID = 3L
    }
}
