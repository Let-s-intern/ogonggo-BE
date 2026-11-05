package com.ogonggo.adminapi.bootcamp.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.bootcamp.business.AdminBootcampPageResult
import com.ogonggo.adminapi.bootcamp.business.AdminBootcampResult
import com.ogonggo.adminapi.bootcamp.business.AdminBootcampService
import com.ogonggo.adminapi.bootcamp.business.AdminBootcampSummary
import com.ogonggo.adminapi.bootcamp.business.AdminBootcampUpdateCommand
import com.ogonggo.adminapi.bootcamp.business.AdminBootcampVisibilityChangeCommand
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.bootcamp.domain.BootcampApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampManagementSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampOperationType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampTuitionType
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.contentreview.domain.ContentSource
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AdminBootcampController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminBootcampControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminBootcampService: AdminBootcampService

    @Test
    fun `모집 상태와 등록 경로 필터를 조회 조건으로 옮긴다`() {
        val condition = BootcampManagementSearchCondition(
            source = ContentSource.CRAWLER,
            status = BootcampRecruitmentStatus.CLOSED,
        )
        Mockito.`when`(adminBootcampService.getBootcamps(condition, BootcampSortType.LATEST, 0, 20))
            .thenReturn(AdminBootcampPageResult(emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0))

        mockMvc.perform(
            admin(get("/api/v1/admin/bootcamps").param("source", "CRAWLER").param("status", "CLOSED")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.pageNum").value(1))
            .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20))
    }

    @Test
    fun `임시저장 모집 상태로는 거를 수 없다`() {
        mockMvc.perform(admin(get("/api/v1/admin/bootcamps").param("status", "DRAFT")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[status] RECRUITING 또는 CLOSED만 고를 수 있습니다."))

        Mockito.verifyNoInteractions(adminBootcampService)
    }

    @Test
    fun `상세 내용은 비울 수 없다`() {
        mockMvc.perform(
            admin(
                patch("/api/v1/admin/bootcamps/3")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("fields" to mapOf("content" to "")))),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[fields.content] 상세 내용은 비울 수 없습니다."))

        Mockito.verifyNoInteractions(adminBootcampService)
    }

    @Test
    fun `고른 부트캠프의 노출을 한꺼번에 바꾸고 data 없이 응답한다`() {
        mockMvc.perform(
            admin(
                patch("/api/v1/admin/bootcamps/visibility")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [5, 2], "visibility": "VISIBLE"}"""),
            ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(adminBootcampService)
            .changeVisibilities(AdminBootcampVisibilityChangeCommand(listOf(5L, 2L), AdminContentVisibility.VISIBLE))
    }

    @Test
    fun `우선 노출 여부를 보내면 수정 명령에 담고 목록과 상세에 저장된 값을 싣는다`() {
        // given
        Mockito.`when`(adminBootcampService.getBootcamp(3L)).thenReturn(detail(summary(id = 3L, priorityExposed = false)))
        Mockito.`when`(adminBootcampService.getBootcamps(BootcampManagementSearchCondition(), BootcampSortType.LATEST, 0, 20))
            .thenReturn(
                AdminBootcampPageResult(
                    listOf(summary(id = 3L, priorityExposed = false), summary(id = 4L, priorityExposed = true)),
                    page = 0,
                    size = 20,
                    totalElements = 2,
                    totalPages = 1,
                ),
            )

        // when
        val patched = mockMvc.perform(
            admin(
                patch("/api/v1/admin/bootcamps/3")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("priorityExposed" to false))),
            ),
        )
        val listed = mockMvc.perform(admin(get("/api/v1/admin/bootcamps")))

        // then
        patched
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.priorityExposed").value(false))
        Mockito.verify(adminBootcampService).updateBootcamp(3L, AdminBootcampUpdateCommand(priorityExposed = false))
        listed
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].priorityExposed").value(false))
            .andExpect(jsonPath("$.data.items[1].priorityExposed").value(true))
    }

    private fun summary(id: Long, priorityExposed: Boolean): AdminBootcampSummary = AdminBootcampSummary(
        id = id,
        companyName = "오공고 교육",
        title = "백엔드 부트캠프",
        programType = "KDT",
        operationType = BootcampOperationType.ONLINE,
        recruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt = LocalDateTime.of(2026, 11, 1, 0, 0),
        recruitmentEndAt = LocalDateTime.of(2026, 11, 30, 23, 59),
        programStartDate = LocalDate.of(2026, 12, 1),
        programEndDate = LocalDate.of(2027, 2, 28),
        capacity = 30,
        tuitionType = BootcampTuitionType.FREE,
        tuitionAmount = null,
        representativeImageUrl = null,
        shortDescription = "백엔드 개발자 과정",
        status = BootcampRecruitmentStatus.RECRUITING,
        closedAt = null,
        viewCount = 0,
        bookmarkCount = 0,
        commentCount = 0,
        visibility = AdminContentVisibility.VISIBLE,
        source = ContentSource.COMPANY,
        reviewStatus = ContentReviewStatus.APPROVED,
        registeredAt = LocalDateTime.of(2026, 11, 1, 10, 0),
        priorityExposed = priorityExposed,
    )

    private fun detail(summary: AdminBootcampSummary): AdminBootcampResult = AdminBootcampResult(
        summary = summary,
        content = "상세 내용",
        eligibilityAndSelectionProcess = null,
        logoUrl = null,
        instructorInfo = null,
        programFeatures = null,
        completionRequirements = null,
        applicationMethod = BootcampApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
        managerEmail = null,
        inquiryUrl = null,
        publicationStartAt = null,
        publicationEndAt = null,
        sourceUrl = null,
        partners = emptyList(),
        curriculums = emptyList(),
        images = emptyList(),
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))
}
