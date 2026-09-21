package com.ogonggo.userapi.job.presentation

import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobBookmarkSearchCondition
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.job.business.UserJobBookmarkService
import com.ogonggo.userapi.job.business.UserJobPageResult
import com.ogonggo.userapi.job.business.UserJobSummary
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UserJobBookmarkController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserJobBookmarkControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userJobBookmarkService: UserJobBookmarkService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `내 북마크 목록을 1 기반 페이지로 조회한다`() {
        Mockito.`when`(userJobBookmarkService.getBookmarks(USER_ID, JobSearchCondition.NONE, 0, 10))
            .thenReturn(bookmarkPage())

        mockMvc.perform(get("/api/v1/job-bookmarks").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(JOB_ID))
            .andExpect(jsonPath("$.data.items[0].bookmarked").value(true))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
    }

    @Test
    fun `내 북마크 목록의 필터와 검색어는 조회 조건으로 전달된다`() {
        // given
        val condition = JobSearchCondition(
            employmentType = EmploymentType.INTERN,
            experienceType = ExperienceType.NEWCOMER,
            jobField = "개발",
            jobRole = "백엔드",
            keyword = "오공고",
        )
        Mockito.`when`(userJobBookmarkService.getBookmarks(USER_ID, condition, 0, 10)).thenReturn(bookmarkPage())

        // when
        mockMvc.perform(
            get("/api/v1/job-bookmarks")
                .param("employmentType", "INTERN")
                .param("experienceType", "NEWCOMER")
                .param("jobField", "개발")
                .param("jobRole", "백엔드")
                .param("keyword", "오공고")
                .with(authenticatedUser()),
        ).andExpect(status().isOk)

        // then
        Mockito.verify(userJobBookmarkService).getBookmarks(USER_ID, condition, 0, 10)
    }

    @Test
    fun `내 북마크 목록의 검색어가 2자 미만이면 400을 반환한다`() {
        mockMvc.perform(get("/api/v1/job-bookmarks").param("keyword", "가").with(authenticatedUser()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(userJobBookmarkService)
    }

    @Test
    fun `북마크를 등록하고 해제한다`() {
        mockMvc.perform(post("/api/v1/job-bookmarks/{jobId}", JOB_ID).with(authenticatedUser()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))

        mockMvc.perform(delete("/api/v1/job-bookmarks/{jobId}", JOB_ID).with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))

        Mockito.verify(userJobBookmarkService).addBookmark(USER_ID, JOB_ID)
        Mockito.verify(userJobBookmarkService).deleteBookmark(USER_ID, JOB_ID)
    }

    @Test
    fun `중복 북마크는 도메인 409 계약으로 응답한다`() {
        Mockito.doThrow(ConflictException(JobErrorCode.JOB_BOOKMARK_ALREADY_EXISTS))
            .`when`(userJobBookmarkService).addBookmark(USER_ID, JOB_ID)

        mockMvc.perform(post("/api/v1/job-bookmarks/{jobId}", JOB_ID).with(authenticatedUser()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("JOB_BOOKMARK_ALREADY_EXISTS"))
            .andExpect(jsonPath("$.message").value("이미 북마크한 일자리 공고입니다."))
    }

    @Test
    fun `인증이 없거나 공고 식별자가 잘못되면 표준 오류로 응답한다`() {
        mockMvc.perform(get("/api/v1/job-bookmarks"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc.perform(post("/api/v1/job-bookmarks/0").with(authenticatedUser()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `지원 단계를 고르면 그 단계만 조회하도록 전달된다`() {
        // given
        Mockito.`when`(
            userJobBookmarkService.getBookmarks(USER_ID, JobSearchCondition.NONE, 0, 10, JobBookmarkSearchCondition(applicationStatus = JobApplicationStatus.PREPARING)),
        ).thenReturn(bookmarkPage())

        // when
        mockMvc.perform(
            get("/api/v1/job-bookmarks").param("applicationStatus", "PREPARING").with(authenticatedUser()),
        ).andExpect(status().isOk)

        // then
        Mockito.verify(userJobBookmarkService).getBookmarks(USER_ID, JobSearchCondition.NONE, 0, 10, JobBookmarkSearchCondition(applicationStatus = JobApplicationStatus.PREPARING))
    }

    @Test
    fun `모집 상태와 정렬은 조회 조건으로 전달되고 정렬 기본값은 최근 저장순이다`() {
        // given
        Mockito.`when`(
            userJobBookmarkService.getBookmarks(
                USER_ID,
                JobSearchCondition.NONE,
                0,
                10,
                JobBookmarkSearchCondition(
                    recruitmentStatus = JobRecruitmentStatus.CLOSED,
                    sortType = BookmarkSortType.RECENTLY_SAVED,
                ),
            ),
        ).thenReturn(bookmarkPage())

        // when
        mockMvc.perform(
            get("/api/v1/job-bookmarks").param("recruitmentStatus", "CLOSED").with(authenticatedUser()),
        ).andExpect(status().isOk)

        // then
        Mockito.verify(userJobBookmarkService).getBookmarks(
            USER_ID,
            JobSearchCondition.NONE,
            0,
            10,
            JobBookmarkSearchCondition(
                recruitmentStatus = JobRecruitmentStatus.CLOSED,
                sortType = BookmarkSortType.RECENTLY_SAVED,
            ),
        )
    }

    @Test
    fun `없는 정렬 기준이면 400을 반환한다`() {
        mockMvc.perform(get("/api/v1/job-bookmarks").param("sort", "DEADLINE").with(authenticatedUser()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(userJobBookmarkService)
    }

    @Test
    fun `북마크를 지원 준비 중으로 옮기고 스크랩으로 되돌린다`() {
        mockMvc.perform(post("/api/v1/job-bookmarks/{id}/prepare", JOB_ID).with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))

        mockMvc.perform(post("/api/v1/job-bookmarks/{id}/cancel-preparation", JOB_ID).with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))

        Mockito.verify(userJobBookmarkService).prepare(USER_ID, JOB_ID)
        Mockito.verify(userJobBookmarkService).cancelPreparation(USER_ID, JOB_ID)
    }

    @Test
    fun `북마크하지 않은 대상의 단계를 옮기면 404로 응답한다`() {
        Mockito.doThrow(EntityNotFoundException(JobErrorCode.JOB_BOOKMARK_NOT_FOUND))
            .`when`(userJobBookmarkService).prepare(USER_ID, JOB_ID)

        mockMvc.perform(post("/api/v1/job-bookmarks/{id}/prepare", JOB_ID).with(authenticatedUser()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("JOB_BOOKMARK_NOT_FOUND"))
    }

    private fun bookmarkPage(): UserJobPageResult = UserJobPageResult(
        items = listOf(
            UserJobSummary(
                id = JOB_ID,
                companyName = "오공고",
                title = "백엔드 개발자",
                employmentType = EmploymentType.FULL_TIME,
                experienceType = ExperienceType.EXPERIENCED,
                experienceMinYears = 1,
                educationLevel = EducationLevel.ANY,
                region = "서울",
                recruitmentType = JobRecruitmentType.PERIOD,
                recruitmentStartAt = null,
                recruitmentEndAt = null,
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
        private const val JOB_ID = 3L
    }
}
