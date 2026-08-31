package com.ogonggo.userapi.job.presentation

import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentType
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
        Mockito.`when`(userJobBookmarkService.getBookmarks(USER_ID, 0, 10)).thenReturn(bookmarkPage())

        mockMvc.perform(get("/api/v1/job-bookmarks").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(JOB_ID))
            .andExpect(jsonPath("$.data.items[0].bookmarked").value(true))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
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

    private fun bookmarkPage(): UserJobPageResult = UserJobPageResult(
        items = listOf(
            UserJobSummary(
                id = JOB_ID,
                companyName = "오공고",
                title = "백엔드 개발자",
                employmentType = EmploymentType.FULL_TIME,
                experienceType = ExperienceType.EXPERIENCED,
                experienceMinYears = 1,
                experienceMaxYears = 3,
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
