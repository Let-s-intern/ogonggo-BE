package com.ogonggo.adminapi.job.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.job.business.AdminJobPageResult
import com.ogonggo.adminapi.job.business.AdminJobService
import com.ogonggo.adminapi.job.business.AdminJobSummary
import com.ogonggo.adminapi.job.business.AdminJobUpdateCommand
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.JobContentField
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.review.error.ReviewErrorCode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
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
            reviewStatus = ReviewStatus.PENDING,
            recruitmentStatus = JobRecruitmentStatus.RECRUITING,
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
                    .param("sort", "VIEW_COUNT"),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(693))
            .andExpect(jsonPath("$.data.items[0].visibility").value("HIDDEN"))
            .andExpect(jsonPath("$.data.items[0].source").value("COMPANY"))
            .andExpect(jsonPath("$.data.items[0].recruitmentStatus").value("RECRUITING"))
            .andExpect(jsonPath("$.data.items[0].registeredAt").value("2026-09-10T10:48:00"))
            .andExpect(jsonPath("$.data.items[0].responsibilities").doesNotExist())
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(2))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
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
                    .param("sort", ""),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
    }

    @Test
    fun `수정은 허용한 본문 칸만 반영하고 빈 문자열은 그 칸을 비우며 수정된 공고를 돌려준다`() {
        Mockito.`when`(adminJobService.getJob(693L)).thenReturn(AdminJobFixtures.detail())

        mockMvc.perform(
            admin(
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
                reviewStatus = ReviewStatus.APPROVED,
                title = "고친 제목",
                contents = mapOf(
                    JobContentField.RESPONSIBILITIES to "고친 업무",
                    JobContentField.COMPENSATION to null,
                ),
            ),
        )
    }

    @Test
    fun `제목을 비우거나 수정 요청으로 반려하면 400으로 응답한다`() {
        mockMvc.perform(admin(patchJson(mapOf("title" to " "))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[title] 제목을 입력해 주세요."))

        mockMvc.perform(admin(patchJson(mapOf("reviewStatus" to "REJECTED"))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[reviewStatus] 반려는 검수 화면에서 사유와 함께 처리해 주세요."))

        Mockito.verifyNoInteractions(adminJobService)
    }

    @Test
    fun `승인 전에 노출하려 하면 409로 응답한다`() {
        Mockito.doThrow(ConflictException(ReviewErrorCode.REVIEW_NOT_APPROVED))
            .`when`(adminJobService)
            .updateJob(693L, AdminJobUpdateCommand(visibility = AdminContentVisibility.VISIBLE))

        mockMvc.perform(admin(patchJson(mapOf("visibility" to "VISIBLE"))))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("REVIEW_NOT_APPROVED"))
    }

    @Test
    fun `삭제하면 data 없이 200으로 응답한다`() {
        mockMvc.perform(admin(delete("/api/v1/admin/jobs/693")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(adminJobService).deleteJob(693L)
    }

    private fun patchJson(body: Map<String, Any>): MockHttpServletRequestBuilder =
        patch("/api/v1/admin/jobs/693")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private fun pageOf(vararg items: AdminJobSummary): AdminJobPageResult =
        AdminJobPageResult(
            items = items.toList(),
            page = if (items.isEmpty()) 0 else 1,
            size = 20,
            totalElements = items.size.toLong(),
            totalPages = if (items.isEmpty()) 0 else 2,
        )
}
