package com.ogonggo.adminapi.job.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.job.business.AdminJobClearableField
import com.ogonggo.adminapi.job.business.AdminJobDetailsPatch
import com.ogonggo.adminapi.job.business.AdminJobPageResult
import com.ogonggo.adminapi.job.business.AdminJobService
import com.ogonggo.adminapi.job.business.AdminJobSummary
import com.ogonggo.adminapi.job.business.AdminTodayJobSummary
import com.ogonggo.adminapi.job.business.AdminJobUpdateCommand
import com.ogonggo.adminapi.job.business.AdminJobVisibilityChangeCommand
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.dto.TodayJobDto
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.contentreview.error.ContentReviewErrorCode
import org.hamcrest.Matchers.startsWith
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AdminJobController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminJobControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminJobService: AdminJobService

    @Test
    fun `목록의 필터와 정렬과 페이지를 조회 조건으로 옮기고 본문 없이 응답한다`() {
        val condition = JobManagementSearchCondition(
            published = false,
            source = ContentSource.COMPANY,
            reviewStatus = ContentReviewStatus.PENDING,
            recruitmentStatus = JobRecruitmentStatus.RECRUITING,
            jobField = JobField.PLANNING_STRATEGY,
            jobRoles = setOf(JobRole.DESIGN_UX_UI_PRODUCT, JobRole.DESIGN_RESEARCH),
            keyword = "후지",
        )
        Mockito.`when`(adminJobService.getJobs(condition, JobSortType.VIEW_COUNT, 1, 20))
            .thenReturn(pageOf(AdminJobFixtures.summary()))

        mockMvc.perform(
            admin(
                get("/api/v1/admin/jobs")
                    .param("page", "2")
                    .param("keyword", "후지")
                    .param("visibility", "HIDDEN")
                    .param("source", "COMPANY")
                    .param("reviewStatus", "PENDING")
                    .param("recruitmentStatus", "RECRUITING")
                    .param("jobField", "PLANNING_STRATEGY")
                    .param("jobRole", "DESIGN_UX_UI_PRODUCT", "DESIGN_RESEARCH")
                    .param("sort", "VIEW_COUNT"),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(693))
            .andExpect(jsonPath("$.data.items[0].visibility").value("HIDDEN"))
            .andExpect(jsonPath("$.data.items[0].source").value("COMPANY"))
            .andExpect(jsonPath("$.data.items[0].jobField").value("PLANNING_STRATEGY"))
            .andExpect(jsonPath("$.data.items[0].jobRole").value("DESIGN_UX_UI_PRODUCT"))
            .andExpect(jsonPath("$.data.items[0].recruitmentStatus").value("RECRUITING"))
            .andExpect(jsonPath("$.data.items[0].registeredAt").value("2026-09-10T10:48:00"))
            .andExpect(jsonPath("$.data.items[0].sourceUrl").value("https://example.com/jobs/693"))
            .andExpect(jsonPath("$.data.items[0].responsibilities").doesNotExist())
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(2))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
    }

    @Test
    fun `등록일 범위와 검수 완료 여부를 조회 조건으로 옮기고 시작일이 늦으면 400이다`() {
        val today = LocalDate.of(2026, 11, 4)
        Mockito.`when`(
            adminJobService.getJobs(
                JobManagementSearchCondition(registeredFrom = today, registeredTo = today, inspected = false),
                JobSortType.LATEST,
                0,
                20,
            ),
        ).thenReturn(pageOf(AdminJobFixtures.summary()))

        mockMvc.perform(
            admin(
                get("/api/v1/admin/jobs")
                    .param("registeredFrom", "2026-11-04")
                    .param("registeredTo", "2026-11-04")
                    .param("inspected", "false"),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(693))

        mockMvc.perform(
            admin(get("/api/v1/admin/jobs").param("registeredFrom", "2026-11-05").param("registeredTo", "2026-11-04")),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(startsWith("[registeredFrom]")))
        mockMvc.perform(admin(get("/api/v1/admin/jobs").param("registeredFrom", "2026-11-04T00:00:00+09:00")))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `빈 필터 값은 보내지 않은 것과 같이 전체로 본다`() {
        Mockito.`when`(adminJobService.getJobs(JobManagementSearchCondition.NONE, JobSortType.LATEST, 0, 20))
            .thenReturn(pageOf())

        mockMvc.perform(
            admin(
                get("/api/v1/admin/jobs")
                    .param("keyword", "")
                    .param("visibility", "")
                    .param("source", "")
                    .param("reviewStatus", "")
                    .param("recruitmentStatus", "")
                    .param("jobField", "")
                    .param("jobRole", "")
                    .param("registeredFrom", "")
                    .param("registeredTo", "")
                    .param("inspected", "")
                    .param("sort", ""),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
    }

    @Test
    fun `상세는 직군과 직무와 본문 칸을 함께 응답한다`() {
        Mockito.`when`(adminJobService.getJob(693L)).thenReturn(AdminJobFixtures.detail())

        mockMvc.perform(admin(get("/api/v1/admin/jobs/693")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(693))
            .andExpect(jsonPath("$.data.jobField").value("PLANNING_STRATEGY"))
            .andExpect(jsonPath("$.data.jobRole").value("DESIGN_UX_UI_PRODUCT"))
            .andExpect(jsonPath("$.data.responsibilities").value("주요 업무"))
    }

    @Test
    fun `상세는 저장된 공고 분석과 지금 본문으로 만든 것인지를 싣고 분석이 없으면 null과 false다`() {
        Mockito.`when`(adminJobService.getJob(693L))
            .thenReturn(AdminJobFixtures.detail(analysis = AdminJobFixtures.ANALYSIS, analysisCurrent = false))
        Mockito.`when`(adminJobService.getJob(7L)).thenReturn(AdminJobFixtures.detail(id = 7L))

        mockMvc.perform(admin(get("/api/v1/admin/jobs/693")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.analysisCurrent").value(false))
            .andExpect(jsonPath("$.data.analysis.tasks[0].tag").value("개발"))
            .andExpect(jsonPath("$.data.analysis.required[0]").value("Kotlin 경험이 있는 분"))
            .andExpect(jsonPath("$.data.analysis.employment.type.value").value("정규직"))
            .andExpect(jsonPath("$.data.analysis.employment.conversion.value").value(null as Any?))
            .andExpect(jsonPath("$.data.analysis.submission.documents.note").value("PDF 권장"))
            .andExpect(jsonPath("$.data.analysis.competencies[0].experiences[0]").value("버그를 끝까지 추적한 경험"))
        mockMvc.perform(admin(get("/api/v1/admin/jobs/7")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.analysis").value(null as Any?))
            .andExpect(jsonPath("$.data.analysisCurrent").value(false))
    }

    @Test
    fun `상세는 운영자가 공고 내용을 마지막으로 고친 일시를 싣고 고친 적이 없으면 null이다`() {
        Mockito.`when`(adminJobService.getJob(693L))
            .thenReturn(AdminJobFixtures.detail(contentEditedAt = LocalDateTime.of(2026, 11, 4, 10, 15)))
        Mockito.`when`(adminJobService.getJob(7L)).thenReturn(AdminJobFixtures.detail(id = 7L))

        mockMvc.perform(admin(get("/api/v1/admin/jobs/693")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.contentEditedAt").value("2026-11-04T10:15:00"))
        mockMvc.perform(admin(get("/api/v1/admin/jobs/7")))
            .andExpect(jsonPath("$.data.contentEditedAt").value(null as Any?))
    }

    @Test
    fun `수정은 허용한 본문 칸만 반영하고 빈 문자열은 그 칸을 비우며 수정된 공고를 돌려준다`() {
        Mockito.`when`(adminJobService.getJob(693L)).thenReturn(AdminJobFixtures.detail())

        mockMvc.perform(
            signedInAdmin(
                patch("/api/v1/admin/jobs/693")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "visibility" to "HIDDEN",
                                "reviewStatus" to "APPROVED",
                                // 등록 경로는 바꿀 수 없어 보내도 버린다.
                                "source" to "CRAWLER",
                                "title" to "고친 제목",
                                "fields" to mapOf(
                                    "responsibilities" to "고친 업무",
                                    "compensation" to "",
                                    "viewCount" to "999",
                                ),
                            ),
                        ),
                    ),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(693))
            .andExpect(jsonPath("$.data.responsibilities").value("주요 업무"))

        Mockito.verify(adminJobService).updateJob(
            693L,
            AdminJobUpdateCommand(
                visibility = AdminContentVisibility.HIDDEN,
                reviewStatus = ContentReviewStatus.APPROVED,
                title = "고친 제목",
                contents = mapOf(
                    JobContentField.RESPONSIBILITIES to "고친 업무",
                    JobContentField.COMPENSATION to null,
                ),
            ),
            ADMIN_USER_ID,
        )
    }

    @Test
    fun `우선 노출 여부를 보내면 수정 명령에 담고 목록과 상세에 저장된 값을 싣는다`() {
        // given
        Mockito.`when`(adminJobService.getJob(693L))
            .thenReturn(AdminJobFixtures.detail().let { it.copy(summary = it.summary.copy(priorityExposed = false)) })
        Mockito.`when`(adminJobService.getJobs(JobManagementSearchCondition(), JobSortType.LATEST, 0, 20))
            .thenReturn(pageOf(AdminJobFixtures.summary(priorityExposed = false), AdminJobFixtures.summary(id = 7L)))

        // when
        val patched = mockMvc.perform(signedInAdmin(patchJson(mapOf("priorityExposed" to false))))
        val listed = mockMvc.perform(admin(get("/api/v1/admin/jobs")))

        // then
        patched
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.priorityExposed").value(false))
        Mockito.verify(adminJobService).updateJob(693L, AdminJobUpdateCommand(priorityExposed = false), ADMIN_USER_ID)
        listed
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].priorityExposed").value(false))
            .andExpect(jsonPath("$.data.items[1].priorityExposed").value(true))
    }

    @Test
    fun `원문 HTML을 보내면 수정 명령에 담고 공백이면 400이다`() {
        Mockito.`when`(adminJobService.getJob(693L)).thenReturn(AdminJobFixtures.detail())

        mockMvc.perform(signedInAdmin(patchJson(mapOf("sourceHtml" to "<p>고친 원문</p>"))))
            .andExpect(status().isOk)
        Mockito.verify(adminJobService).updateJob(693L, AdminJobUpdateCommand(sourceHtml = "<p>고친 원문</p>"), ADMIN_USER_ID)

        mockMvc.perform(signedInAdmin(patchJson(mapOf("sourceHtml" to " "))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(startsWith("[sourceHtml]")))
    }

    @Test
    fun `공고 정보와 clear를 보내면 바꿀 값과 비울 값으로 나눠 넘기고 상세에 공고 정보를 싣는다`() {
        Mockito.`when`(adminJobService.getJob(693L)).thenReturn(AdminJobFixtures.detail())

        mockMvc.perform(
            signedInAdmin(
                patchJson(
                    mapOf(
                        "experienceType" to "EXPERIENCED",
                        "experienceMinYears" to 3,
                        "recruitmentEndAt" to "2026-09-30T23:59:00",
                        "jobRole" to "IT_BACKEND",
                        "clear" to listOf("recruitmentHeadcount", "applyEmail"),
                    ),
                ),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.experienceMinYears").value(3))

        Mockito.verify(adminJobService).updateJob(
            693L,
            AdminJobUpdateCommand(
                details = AdminJobDetailsPatch(
                    experienceType = JobExperienceType.EXPERIENCED,
                    experienceMinYears = 3,
                    recruitmentEndAt = LocalDateTime.of(2026, 9, 30, 23, 59),
                    jobRole = JobRole.IT_BACKEND,
                    cleared = setOf(AdminJobClearableField.RECRUITMENT_HEADCOUNT, AdminJobClearableField.APPLY_EMAIL),
                ),
            ),
            ADMIN_USER_ID,
        )
    }

    @Test
    fun `비울 수 없는 이름을 clear에 담거나 보낸 값을 함께 비우거나 공백만 보내면 400으로 응답한다`() {
        mockMvc.perform(signedInAdmin(patchJson(mapOf("clear" to listOf("companyName")))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[clear] 비울 수 없는 필드입니다: companyName"))

        mockMvc.perform(signedInAdmin(patchJson(mapOf("experienceMinYears" to 2, "clear" to listOf("experienceMinYears")))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[experienceMinYears] 값을 보내면서 비울 수 없습니다."))

        mockMvc.perform(signedInAdmin(patchJson(mapOf("industry" to " "))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[industry] 공백일 수 없습니다. 비우려면 clear에 담아 주세요."))

        mockMvc.perform(signedInAdmin(patchJson(mapOf("experienceMinYears" to -1))))
            .andExpect(status().isBadRequest)

        Mockito.verifyNoInteractions(adminJobService)
    }

    @Test
    fun `제목을 비우거나 수정 요청으로 반려하면 400으로 응답한다`() {
        mockMvc.perform(signedInAdmin(patchJson(mapOf("title" to " "))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[title] 제목을 입력해 주세요."))

        mockMvc.perform(signedInAdmin(patchJson(mapOf("reviewStatus" to "REJECTED"))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[reviewStatus] 반려는 검수 화면에서 사유와 함께 처리해 주세요."))

        Mockito.verifyNoInteractions(adminJobService)
    }

    @Test
    fun `승인 전에 노출하려 하면 409로 응답한다`() {
        Mockito.doThrow(ConflictException(ContentReviewErrorCode.REVIEW_NOT_APPROVED))
            .`when`(adminJobService)
            .updateJob(693L, AdminJobUpdateCommand(visibility = AdminContentVisibility.VISIBLE), ADMIN_USER_ID)

        mockMvc.perform(signedInAdmin(patchJson(mapOf("visibility" to "VISIBLE"))))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("REVIEW_NOT_APPROVED"))
    }

    @Test
    fun `분석 수정은 분석 칸을 받아 로그인한 관리자로 저장하고 바뀐 공고를 돌려준다`() {
        Mockito.`when`(adminJobService.getJob(693L))
            .thenReturn(AdminJobFixtures.detail(analysis = AdminJobFixtures.ANALYSIS, analysisCurrent = true))

        mockMvc.perform(
            signedInAdmin(
                put("/api/v1/admin/jobs/693/analysis")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(analysisBody())),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.analysisCurrent").value(true))
            .andExpect(jsonPath("$.data.analysis.required[0]").value("Kotlin 경험이 있는 분"))

        Mockito.verify(adminJobService).replaceAnalysis(693L, AdminJobFixtures.ANALYSIS, ADMIN_USER_ID)
    }

    @Test
    fun `분석 수정은 AI 서버 저장과 같이 항목 개수와 빈 칸을 검증해 400으로 거절한다`() {
        val tooManyTasks = analysisBody().plus(
            "tasks" to List(4) { mapOf("tag" to "개발", "text" to "서버를 개발해요.") },
        )
        val blankTaskTag = analysisBody().plus("tasks" to listOf(mapOf("tag" to " ", "text" to "서버를 개발해요.")))
        val missingEmployment = analysisBody().minus("employment")
        // 목록 원소의 검증은 Kotlin 1.9 에서 형식 인자 주석이 남지 않아 Bean Validation 이 보지 못한다. 도메인이 막는다.
        val blankRequired = analysisBody().plus("required" to listOf(" "))
        val tooLongPreferred = analysisBody().plus("preferred" to listOf("가".repeat(1001)))
        @Suppress("UNCHECKED_CAST")
        val competency = (analysisBody()["competencies"] as List<Map<String, Any?>>).single()
        val blankExperience = analysisBody().plus("competencies" to listOf(competency.plus("experiences" to listOf(""))))

        listOf(tooManyTasks, blankTaskTag, missingEmployment, blankRequired, tooLongPreferred, blankExperience).forEach { body ->
            mockMvc.perform(
                signedInAdmin(
                    put("/api/v1/admin/jobs/693/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)),
                ),
            )
                .andExpect(status().isBadRequest)
        }
        Mockito.verifyNoInteractions(adminJobService)
    }

    @Test
    fun `검수 완료 표시는 본문 없이 받아 로그인한 관리자로 남기고 data 없이 200으로 응답한다`() {
        mockMvc.perform(signedInAdmin(put("/api/v1/admin/jobs/693/inspection")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(adminJobService).markInspected(693L, ADMIN_USER_ID)
    }

    @Test
    fun `검수 완료 해제는 data 없이 200으로 응답하고 없는 공고는 404다`() {
        mockMvc.perform(admin(delete("/api/v1/admin/jobs/693/inspection")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())
        Mockito.verify(adminJobService).clearInspection(693L)

        Mockito.doThrow(EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)).`when`(adminJobService).clearInspection(999L)
        mockMvc.perform(admin(delete("/api/v1/admin/jobs/999/inspection")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"))
    }

    @Test
    fun `목록과 상세는 검수 완료 일시를 싣고 미검수면 null이다`() {
        Mockito.`when`(adminJobService.getJobs(JobManagementSearchCondition.NONE, JobSortType.LATEST, 0, 20))
            .thenReturn(pageOf(AdminJobFixtures.summary(inspectedAt = LocalDateTime.of(2026, 11, 4, 9, 30))))
        Mockito.`when`(adminJobService.getJob(693L)).thenReturn(AdminJobFixtures.detail())

        mockMvc.perform(admin(get("/api/v1/admin/jobs")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].inspectedAt").value("2026-11-04T09:30:00"))
        mockMvc.perform(admin(get("/api/v1/admin/jobs/693")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.inspectedAt").value(null as Any?))
    }

    @Test
    fun `삭제하면 data 없이 200으로 응답한다`() {
        mockMvc.perform(admin(delete("/api/v1/admin/jobs/693")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(adminJobService).deleteJob(693L)
    }

    @Test
    fun `오늘의 공고는 페이지 정보 없이 고른 순서대로 추천 문구와 함께 응답한다`() {
        Mockito.`when`(adminJobService.getTodayJobs())
            .thenReturn(listOf(todayJobSummary(7L), todayJobSummary(3L)))

        mockMvc.perform(admin(get("/api/v1/admin/jobs/today")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].id").value(7))
            .andExpect(jsonPath("$.data[1].id").value(3))
            .andExpect(jsonPath("$.data[0].visibility").value("HIDDEN"))
            .andExpect(jsonPath("$.data[0].sourceUrl").value("https://example.com/jobs/7"))
            .andExpect(jsonPath("$.data[0].recommendationTitle").value("경력 없이 시작하고 싶다면"))
            .andExpect(jsonPath("$.data[0].recommendationDescription").value("실무 중심 프로젝트로 빠른 성장"))
            .andExpect(jsonPath("$.data[0].job").doesNotExist())
    }

    @Test
    fun `오늘의 공고를 보낸 순서대로 추천 문구와 함께 바꾸고 바뀐 목록을 돌려준다`() {
        Mockito.`when`(adminJobService.getTodayJobs())
            .thenReturn(listOf(todayJobSummary(7L), todayJobSummary(3L)))

        mockMvc.perform(admin(putTodayJobs(todayJobsBody(7, 3))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(7))
            .andExpect(jsonPath("$.data[1].id").value(3))

        Mockito.verify(adminJobService).replaceTodayJobs(
            listOf(
                TodayJobDto.Request(7L, "경력 없이 시작하고 싶다면", "실무 중심 프로젝트로 빠른 성장"),
                TodayJobDto.Request(3L, "경력 없이 시작하고 싶다면", "실무 중심 프로젝트로 빠른 성장"),
            ),
        )
    }

    @Test
    fun `빈 배열을 보내면 오늘의 공고를 비운다`() {
        Mockito.`when`(adminJobService.getTodayJobs()).thenReturn(emptyList())

        mockMvc.perform(admin(putTodayJobs("""{"jobs": []}""")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)

        Mockito.verify(adminJobService).replaceTodayJobs(emptyList())
    }

    @Test
    fun `오늘의 공고에 같은 공고를 두 번 넣거나 비운 항목을 넣거나 목록을 빼면 400으로 응답한다`() {
        mockMvc.perform(admin(putTodayJobs(todayJobsBody(7, 7))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[jobs] 같은 공고를 두 번 넣을 수 없습니다."))

        mockMvc.perform(admin(putTodayJobs("""{"jobs": [null]}""")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[jobs] 오늘의 공고 항목을 비울 수 없습니다."))

        mockMvc.perform(admin(putTodayJobs("{}")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(adminJobService)
    }

    @Test
    fun `오늘의 공고 항목의 식별자가 양수가 아니거나 추천 문구가 비었거나 길면 400으로 응답한다`() {
        listOf(
            todayJobItem(jobId = 0) to "jobs[0].jobId",
            todayJobItem(title = " ") to "jobs[0].recommendationTitle",
            todayJobItem(title = "가".repeat(31)) to "jobs[0].recommendationTitle",
            todayJobItem(description = "") to "jobs[0].recommendationDescription",
            todayJobItem(description = "가".repeat(201)) to "jobs[0].recommendationDescription",
        ).forEach { (item, field) ->
            mockMvc.perform(admin(putTodayJobs("""{"jobs": [$item]}""")))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value(startsWith("[$field]")))
        }

        Mockito.verifyNoInteractions(adminJobService)
    }

    @Test
    fun `오늘의 공고에 없는 공고를 넣으면 404로 응답한다`() {
        Mockito.doThrow(EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND))
            .`when`(adminJobService)
            .replaceTodayJobs(listOf(TodayJobDto.Request(999L, "경력 없이 시작하고 싶다면", "실무 중심 프로젝트로 빠른 성장")))

        mockMvc.perform(admin(putTodayJobs(todayJobsBody(999))))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"))
    }

    @Test
    fun `관리자 토큰 없이는 오늘의 공고를 설정할 수 없다`() {
        mockMvc.perform(putTodayJobs(todayJobsBody(7)))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(adminJobService)
    }

    @Test
    fun `고른 공고의 노출을 한꺼번에 바꾸고 data 없이 응답한다`() {
        mockMvc.perform(admin(patchVisibility("""{"ids": [7, 3, 7], "visibility": "HIDDEN"}""")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(adminJobService)
            .changeVisibilities(AdminJobVisibilityChangeCommand(listOf(7L, 3L, 7L), AdminContentVisibility.HIDDEN))
    }

    @Test
    fun `노출 일괄 변경에 식별자가 없거나 너무 많거나 양수가 아니거나 노출 값이 없으면 400으로 응답한다`() {
        val tooMany = (1..1001).joinToString(prefix = "[", postfix = "]")
        listOf("[]", tooMany).forEach { ids ->
            mockMvc.perform(admin(patchVisibility("""{"ids": $ids, "visibility": "HIDDEN"}""")))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value(startsWith("[ids]")))
        }
        listOf("[0]", "[null]").forEach { ids ->
            mockMvc.perform(admin(patchVisibility("""{"ids": $ids, "visibility": "HIDDEN"}""")))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("[ids] 채용공고 식별자는 양수여야 합니다."))
        }
        mockMvc.perform(admin(patchVisibility("""{"ids": [7]}""")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        Mockito.verifyNoInteractions(adminJobService)
    }

    @Test
    fun `노출 일괄 변경에 없는 공고가 섞이면 그 식별자를 메시지에 담아 404로 응답한다`() {
        Mockito.doThrow(EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND, "일자리 공고를 찾을 수 없습니다. (id: 999)"))
            .`when`(adminJobService)
            .changeVisibilities(AdminJobVisibilityChangeCommand(listOf(7L, 999L), AdminContentVisibility.VISIBLE))

        mockMvc.perform(admin(patchVisibility("""{"ids": [7, 999], "visibility": "VISIBLE"}""")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("일자리 공고를 찾을 수 없습니다. (id: 999)"))
    }

    private fun patchVisibility(body: String): MockHttpServletRequestBuilder =
        patch("/api/v1/admin/jobs/visibility").contentType(MediaType.APPLICATION_JSON).content(body)

    private fun putTodayJobs(body: String): MockHttpServletRequestBuilder =
        put("/api/v1/admin/jobs/today").contentType(MediaType.APPLICATION_JSON).content(body)

    private fun todayJobsBody(vararg jobIds: Long): String =
        """{"jobs": [${jobIds.joinToString { todayJobItem(jobId = it) }}]}"""

    private fun todayJobItem(
        jobId: Long = 7,
        title: String = "경력 없이 시작하고 싶다면",
        description: String = "실무 중심 프로젝트로 빠른 성장",
    ): String = objectMapper.writeValueAsString(
        mapOf("jobId" to jobId, "recommendationTitle" to title, "recommendationDescription" to description),
    )

    private fun todayJobSummary(id: Long): AdminTodayJobSummary =
        AdminTodayJobSummary(AdminJobFixtures.summary(id), "경력 없이 시작하고 싶다면", "실무 중심 프로젝트로 빠른 성장")

    private fun patchJson(body: Map<String, Any>): MockHttpServletRequestBuilder =
        patch("/api/v1/admin/jobs/693")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    /** 관리자 인증 필터처럼 사용자 식별자를 주체로 남긴다. */
    private fun signedInAdmin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(
            authentication(
                UsernamePasswordAuthenticationToken(ADMIN_USER_ID, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY))),
            ),
        )

    private fun pageOf(vararg items: AdminJobSummary): AdminJobPageResult =
        AdminJobPageResult(
            items = items.toList(),
            page = if (items.isEmpty()) 0 else 1,
            size = 20,
            totalElements = items.size.toLong(),
            totalPages = if (items.isEmpty()) 0 else 2,
        )

    /** [AdminJobFixtures.ANALYSIS]와 같은 분석을 요청 JSON 모양으로 둔다. 비운 칸은 공백으로 보내 null로 저장되는지도 본다. */
    private fun analysisBody(): Map<String, Any?> {
        fun fact(value: String?, note: String? = null) = mapOf("value" to value, "note" to note)
        return mapOf(
            "tasks" to listOf(mapOf("tag" to "개발", "text" to "서버를 개발해요.")),
            "required" to listOf("Kotlin 경험이 있는 분"),
            "preferred" to emptyList<String>(),
            "employment" to mapOf(
                "type" to fact("정규직"),
                "conversion" to fact(" "),
                "salary" to fact(null),
                "affiliation" to fact(null),
            ),
            "submission" to mapOf(
                "documents" to fact("이력서", "PDF 권장"),
                "essay" to fact(null),
                "process" to fact(null),
                "deadline" to fact(null),
            ),
            "competencies" to listOf(
                mapOf(
                    "name" to "문제 해결",
                    "quote" to "주요 업무",
                    "description" to "막힌 문제를 끝까지 푸는 역량이에요.",
                    "experiences" to listOf("버그를 끝까지 추적한 경험"),
                ),
            ),
        )
    }

    private companion object {
        const val ADMIN_USER_ID = 1L
    }
}
