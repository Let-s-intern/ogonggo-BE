package com.ogonggo.userapi.presentation

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.UnauthorizedException
import com.ogonggo.core.job.domain.EducationLevel
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.userapi.bootcamp.business.UserBootcampCurriculumResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampPageResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampPartnerResult
import com.ogonggo.userapi.bootcamp.business.UserBootcampResult
import com.ogonggo.userapi.auth.error.AuthErrorCode
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.bootcamp.business.UserBootcampService
import com.ogonggo.userapi.bootcamp.business.UserBootcampSummary
import com.ogonggo.userapi.bootcamp.presentation.UserBootcampController
import com.ogonggo.userapi.error.UserApiExceptionHandler
import com.ogonggo.userapi.config.UserSecurityConfiguration
import com.ogonggo.userapi.job.business.UserJobPageResult
import com.ogonggo.userapi.job.business.UserJobCalendarItem
import com.ogonggo.userapi.job.business.UserJobResult
import com.ogonggo.userapi.job.business.UserJobService
import com.ogonggo.userapi.job.business.UserJobSummary
import com.ogonggo.userapi.job.presentation.UserJobController
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [UserJobController::class, UserBootcampController::class])
@Import(UserSecurityConfiguration::class, UserApiExceptionHandler::class)
class UserReadControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var userJobService: UserJobService

    @MockBean
    private lateinit var userBootcampService: UserBootcampService

    @MockBean
    private lateinit var ogonggoTokenProvider: OgonggoTokenProvider

    @Test
    fun `인증 사용자는 공고 목록과 상세를 조회한다`() {
        Mockito.`when`(userJobService.getJobs(USER_ID, 0, 10, JobSortType.LATEST)).thenReturn(jobPageResult())
        Mockito.`when`(userJobService.getJob(USER_ID, 1L)).thenReturn(jobResult())

        mockMvc.perform(get("/api/v1/jobs").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
            .andExpect(jsonPath("$.data.items[0].id").value(1))
            .andExpect(jsonPath("$.data.items[0].bookmarked").value(true))
            .andExpect(jsonPath("$.data.items[0].viewCount").value(12))
            .andExpect(jsonPath("$.data.items[0].bookmarkCount").value(3))
            .andExpect(jsonPath("$.data.items[0].commentCount").value(0))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
            .andExpect(jsonPath("$.data.pageInfo.totalPages").value(1))
            .andExpect(jsonPath("$.data.page").doesNotExist())
            .andExpect(jsonPath("$.data.hasNext").doesNotExist())
            .andExpect(jsonPath("$.code").doesNotExist())

        mockMvc.perform(get("/api/v1/jobs/1").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
            .andExpect(jsonPath("$.data.title").value("백엔드 개발자"))
            .andExpect(jsonPath("$.data.responsibilities").value("주요 업무"))
            .andExpect(jsonPath("$.data.qualifications").value("자격 요건"))
            .andExpect(jsonPath("$.data.bookmarked").value(true))
            .andExpect(jsonPath("$.data.viewCount").value(12))
            .andExpect(jsonPath("$.data.bookmarkCount").value(3))
            .andExpect(jsonPath("$.data.commentCount").value(0))
            .andExpect(jsonPath("$.data.content").doesNotExist())
    }

    @Test
    fun `인증 사용자는 부트캠프 목록과 상세를 조회한다`() {
        Mockito.`when`(userBootcampService.getBootcamps(0, 10, BootcampSortType.LATEST)).thenReturn(bootcampPageResult())
        Mockito.`when`(userBootcampService.getBootcamp(1L)).thenReturn(bootcampResult())

        mockMvc.perform(get("/api/v1/bootcamps").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
            .andExpect(jsonPath("$.data.items[0].representativeImageUrl").value("https://example.com/image.png"))
            .andExpect(jsonPath("$.data.items[0].viewCount").value(21))
            .andExpect(jsonPath("$.data.items[0].bookmarkCount").value(5))
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))

        mockMvc.perform(get("/api/v1/bootcamps/1").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
            .andExpect(jsonPath("$.data.partners[0].name").value("파트너사"))
            .andExpect(jsonPath("$.data.curriculums[0].subtitle").value("Spring 기초"))
            .andExpect(jsonPath("$.data.viewCount").value(21))
            .andExpect(jsonPath("$.data.bookmarkCount").value(5))
            .andExpect(jsonPath("$.data.commentCount").value(0))
    }

    @Test
    fun `외부 페이지 번호는 서비스 호출 전에 0 기반으로 변환한다`() {
        Mockito.`when`(userJobService.getJobs(USER_ID, 1, 15, JobSortType.LATEST)).thenReturn(
            jobPageResult(page = 1, size = 15),
        )

        mockMvc.perform(
            get("/api/v1/jobs")
                .param("page", "2")
                .param("size", "15")
                .with(authenticatedUser()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(2))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(15))

        Mockito.verify(userJobService).getJobs(USER_ID, 1, 15, JobSortType.LATEST)
    }

    @Test
    fun `정렬을 지정하지 않으면 최신순으로 조회하고 조회수순도 고를 수 있다`() {
        Mockito.`when`(userJobService.getJobs(USER_ID, 0, 10, JobSortType.LATEST)).thenReturn(jobPageResult())
        Mockito.`when`(userJobService.getJobs(USER_ID, 0, 10, JobSortType.VIEW_COUNT)).thenReturn(jobPageResult())
        Mockito.`when`(userBootcampService.getBootcamps(0, 10, BootcampSortType.VIEW_COUNT))
            .thenReturn(bootcampPageResult())

        mockMvc.perform(get("/api/v1/jobs").with(authenticatedUser()))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/jobs").param("sort", "VIEW_COUNT").with(authenticatedUser()))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/bootcamps").param("sort", "VIEW_COUNT").with(authenticatedUser()))
            .andExpect(status().isOk)

        Mockito.verify(userJobService).getJobs(USER_ID, 0, 10, JobSortType.LATEST)
        Mockito.verify(userJobService).getJobs(USER_ID, 0, 10, JobSortType.VIEW_COUNT)
        Mockito.verify(userBootcampService).getBootcamps(0, 10, BootcampSortType.VIEW_COUNT)
    }

    @Test
    fun `원문 이동을 기록하고 반복 호출도 성공으로 응답한다`() {
        mockMvc.perform(post("/api/v1/jobs/1/source-url-clicks").with(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(post("/api/v1/jobs/1/source-url-clicks").with(authenticatedUser()))
            .andExpect(status().isOk)

        Mockito.verify(userJobService, Mockito.times(2)).recordSourceUrlClick(USER_ID, 1L)
    }

    @Test
    fun `게시되지 않은 공고의 원문 이동은 404로 응답한다`() {
        Mockito.doThrow(EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND))
            .`when`(userJobService).recordSourceUrlClick(USER_ID, 9L)

        mockMvc.perform(post("/api/v1/jobs/9/source-url-clicks").with(authenticatedUser()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"))
    }

    @Test
    fun `인증 없이 원문 이동을 기록할 수 없다`() {
        mockMvc.perform(post("/api/v1/jobs/1/source-url-clicks"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `정의되지 않은 정렬 값은 표준 400 오류로 응답한다`() {
        mockMvc.perform(get("/api/v1/jobs").param("sort", "POPULAR").with(authenticatedUser()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `인증 사용자는 기간과 겹치는 공고 달력을 조회한다`() {
        val from = LocalDate.of(2026, 8, 1)
        val to = LocalDate.of(2026, 8, 31)
        Mockito.`when`(userJobService.getJobCalendar(from, to)).thenReturn(
            listOf(
                UserJobCalendarItem(
                    id = 1L,
                    companyName = "오공고",
                    recruitmentStartAt = LocalDateTime.of(2026, 8, 10, 9, 0),
                    recruitmentEndAt = LocalDateTime.of(2026, 8, 31, 23, 59),
                ),
            ),
        )

        mockMvc.perform(
            get("/api/v1/jobs/calendar")
                .param("from", "2026-08-01")
                .param("to", "2026-08-31")
                .with(authenticatedUser()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].companyName").value("오공고"))
            .andExpect(jsonPath("$.data[0].recruitmentStartAt").value("2026-08-10T09:00:00"))
            .andExpect(jsonPath("$.data[0].recruitmentEndAt").value("2026-08-31T23:59:00"))
            .andExpect(jsonPath("$.data[0].title").doesNotExist())
            .andExpect(jsonPath("$.data[0].sourceUrl").doesNotExist())
    }

    @Test
    fun `달력 조회 시작일이 종료일보다 늦으면 파라미터명이 포함된 400을 반환한다`() {
        mockMvc.perform(
            get("/api/v1/jobs/calendar")
                .param("from", "2026-09-01")
                .param("to", "2026-08-31")
                .with(authenticatedUser()),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value(startsWith("[from] ")))

        Mockito.verifyNoInteractions(userJobService)
    }

    @Test
    fun `달력 조회 기간이 92일을 넘으면 400을 반환한다`() {
        mockMvc.perform(
            get("/api/v1/jobs/calendar")
                .param("from", "2026-01-01")
                .param("to", "2026-04-03")
                .with(authenticatedUser()),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value(startsWith("[to] ")))

        Mockito.verifyNoInteractions(userJobService)
    }

    @Test
    fun `달력 조회 기간이 정확히 92일이면 조회한다`() {
        val from = LocalDate.of(2026, 1, 1)
        val to = LocalDate.of(2026, 4, 2)
        Mockito.`when`(userJobService.getJobCalendar(from, to)).thenReturn(emptyList())

        mockMvc.perform(
            get("/api/v1/jobs/calendar")
                .param("from", from.toString())
                .param("to", to.toString())
                .with(authenticatedUser()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    fun `익명 사용자도 공고와 부트캠프 조회 API를 호출할 수 있다`() {
        Mockito.`when`(userJobService.getJobs(null, 0, 10, JobSortType.LATEST)).thenReturn(jobPageResult())
        Mockito.`when`(userJobService.getJob(null, 1L)).thenReturn(jobResult())
        Mockito.`when`(userJobService.getJobCalendar(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
            .thenReturn(emptyList())
        Mockito.`when`(userBootcampService.getBootcamps(0, 10, BootcampSortType.LATEST)).thenReturn(bootcampPageResult())
        Mockito.`when`(userBootcampService.getBootcamp(1L)).thenReturn(bootcampResult())

        listOf(
            "/api/v1/jobs",
            "/api/v1/jobs/calendar?from=2026-08-01&to=2026-08-31",
            "/api/v1/jobs/1",
            "/api/v1/bootcamps",
            "/api/v1/bootcamps/1",
        ).forEach { path ->
            mockMvc.perform(get(path))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").doesNotExist())
        }
    }

    @Test
    fun `익명 조회는 사용자 식별자 없이 서비스를 호출한다`() {
        Mockito.`when`(userJobService.getJobs(null, 0, 10, JobSortType.LATEST))
            .thenReturn(jobPageResult(bookmarked = false))
        Mockito.`when`(userJobService.getJob(null, 1L)).thenReturn(jobResult(bookmarked = false))

        mockMvc.perform(get("/api/v1/jobs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].bookmarked").value(false))

        mockMvc.perform(get("/api/v1/jobs/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.bookmarked").value(false))

        Mockito.verify(userJobService).getJobs(null, 0, 10, JobSortType.LATEST)
        Mockito.verify(userJobService).getJob(null, 1L)
    }

    @Test
    fun `유효하지 않은 토큰으로 조회하면 401 대신 비로그인 응답을 준다`() {
        Mockito.`when`(ogonggoTokenProvider.parseAccessToken("not-a-real-token"))
            .thenThrow(UnauthorizedException(AuthErrorCode.INVALID_TOKEN))
        Mockito.`when`(userJobService.getJobs(null, 0, 10, JobSortType.LATEST))
            .thenReturn(jobPageResult(bookmarked = false))

        mockMvc.perform(get("/api/v1/jobs").header("Authorization", "Bearer not-a-real-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].bookmarked").value(false))
    }

    @Test
    fun `권한이 없는 인증 사용자는 표준 403 응답을 반환한다`() {
        mockMvc.perform(get("/not-allowed").with(authenticatedUser()))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("리소스 접근 권한이 없습니다."))
    }

    @Test
    fun `잘못된 페이지 요청은 400을 반환한다`() {
        mockMvc.perform(
            get("/api/v1/jobs")
                .param("page", "0")
                .param("size", "101")
                .with(authenticatedUser()),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        mockMvc.perform(
            get("/api/v1/jobs")
                .param("page", "not-a-number")
                .with(authenticatedUser()),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `존재하지 않는 공개 데이터는 404를 반환한다`() {
        Mockito.`when`(userJobService.getJob(USER_ID, 99L))
            .thenThrow(EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND))
        Mockito.`when`(userBootcampService.getBootcamp(99L))
            .thenThrow(EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND))

        mockMvc.perform(get("/api/v1/jobs/99").with(authenticatedUser()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("일자리 공고를 찾을 수 없습니다."))

        mockMvc.perform(get("/api/v1/bootcamps/99").with(authenticatedUser()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("BOOTCAMP_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("부트캠프를 찾을 수 없습니다."))
    }

    private fun jobPageResult(
        page: Int = 0,
        size: Int = 10,
        bookmarked: Boolean = true,
    ): UserJobPageResult = UserJobPageResult(
        items = listOf(jobSummary(bookmarked)),
        page = page,
        size = size,
        totalElements = 1,
        totalPages = 1,
        hasNext = false,
    )

    private fun jobSummary(bookmarked: Boolean = true): UserJobSummary = UserJobSummary(
        id = 1L,
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
        bookmarked = bookmarked,
        viewCount = 12,
        bookmarkCount = 3,
        commentCount = 0,
    )

    private fun jobResult(bookmarked: Boolean = true): UserJobResult = UserJobResult(
        id = 1L,
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
        companyAndTeamIntroduction = "회사 및 팀 소개",
        responsibilities = "주요 업무",
        qualifications = "자격 요건",
        preferredQualifications = "우대 사항",
        compensation = "급여 및 처우",
        benefits = "복지 및 혜택",
        hiringProcess = "채용 절차",
        sourceUrl = null,
        closedAt = null,
        bookmarked = bookmarked,
        viewCount = 12,
        bookmarkCount = 3,
        commentCount = 0,
    )

    private fun authenticatedUser() = authentication(
        UsernamePasswordAuthenticationToken(USER_ID, null, emptyList()),
    )

    private fun bootcampPageResult(): UserBootcampPageResult = UserBootcampPageResult(
        items = listOf(bootcampSummary()),
        page = 0,
        size = 10,
        totalElements = 1,
        totalPages = 1,
        hasNext = false,
    )

    private fun bootcampSummary(): UserBootcampSummary = UserBootcampSummary(
        id = 1L,
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
        viewCount = 21,
        bookmarkCount = 5,
        commentCount = 0,
    )

    private fun bootcampResult(): UserBootcampResult = UserBootcampResult(
        id = 1L,
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
        content = "부트캠프 상세 내용",
        eligibilityAndSelectionProcess = null,
        applicationMethod = ApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
        managerEmail = null,
        inquiryUrl = null,
        publicationStartAt = null,
        publicationEndAt = null,
        sourceUrl = null,
        status = BootcampStatus.RECRUITING,
        closedAt = null,
        viewCount = 21,
        bookmarkCount = 5,
        commentCount = 0,
        partners = listOf(UserBootcampPartnerResult("파트너사", 0)),
        curriculums = listOf(UserBootcampCurriculumResult(1, 4, "Spring 기초", 0)),
    )

    companion object {
        private const val USER_ID = 17L
    }
}
