package com.ogonggo.adminapi.review.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.review.business.AdminRejectionService
import com.ogonggo.adminapi.review.business.AdminReviewDecisionResult
import com.ogonggo.adminapi.review.business.AdminReviewItem
import com.ogonggo.adminapi.review.business.AdminReviewMeta
import com.ogonggo.adminapi.review.business.AdminReviewSection
import com.ogonggo.adminapi.review.business.AdminReviewService
import com.ogonggo.core.review.domain.ReviewContentType
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.review.implement.dto.ContentRejectionDto
import com.ogonggo.core.review.implement.dto.ContentRejectionPageDto
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
import java.time.LocalDateTime

@WebMvcTest(controllers = [AdminReviewQueueController::class, AdminRejectionController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminReviewControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminReviewService: AdminReviewService

    @MockBean
    private lateinit var adminRejectionService: AdminRejectionService

    @Test
    fun `검수 대기는 페이지 없이 배열로 준다`() {
        Mockito.`when`(adminReviewService.getQueue()).thenReturn(
            listOf(
                AdminReviewItem(
                    type = ReviewContentType.JOB,
                    id = 693,
                    title = "VMD 경력사원 채용",
                    companyName = "한국후지필름",
                    registeredAt = LocalDateTime.of(2026, 9, 10, 10, 48),
                    sourceUrl = null,
                    meta = listOf(AdminReviewMeta("고용 형태", "계약직")),
                    sections = listOf(AdminReviewSection("responsibilities", "주요 업무", "본문")),
                ),
            ),
        )

        mockMvc.perform(admin(get("/api/v1/admin/review-queue")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].type").value("JOB"))
            .andExpect(jsonPath("$.data[0].meta[0].value").value("계약직"))
            .andExpect(jsonPath("$.data[0].sections[0].field").value("responsibilities"))
    }

    @Test
    fun `승인하면 남은 검수 대기 건수와 함께 응답한다`() {
        Mockito.`when`(adminReviewService.approve(ReviewContentType.BOOTCAMP, 3L))
            .thenReturn(AdminReviewDecisionResult(ReviewContentType.BOOTCAMP, 3L, ReviewStatus.APPROVED, 14))

        mockMvc.perform(admin(decide("bootcamp", mapOf("decision" to "APPROVED"))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.type").value("BOOTCAMP"))
            .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"))
            .andExpect(jsonPath("$.data.remaining").value(14))
    }

    @Test
    fun `반려에 사유가 없거나 판정이 검수 대기이거나 종류가 잘못되면 400으로 응답한다`() {
        mockMvc.perform(admin(decide("job", mapOf("decision" to "REJECTED", "reason" to " "))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[reason] 반려 사유를 입력해 주세요."))

        mockMvc.perform(admin(decide("job", mapOf("decision" to "PENDING"))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[decision] APPROVED 또는 REJECTED만 보낼 수 있습니다."))

        mockMvc.perform(admin(decide("side-study", mapOf("decision" to "APPROVED"))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[type] job 또는 bootcamp만 쓸 수 있습니다."))

        Mockito.verifyNoInteractions(adminReviewService)
    }

    @Test
    fun `반려는 사유와 함께 넘기고 되돌리기는 검수 대기로 돌린다`() {
        Mockito.`when`(adminReviewService.reject(ReviewContentType.JOB, 693L, "급여 조건이 비어 있습니다."))
            .thenReturn(AdminReviewDecisionResult(ReviewContentType.JOB, 693L, ReviewStatus.REJECTED, 2))
        Mockito.`when`(adminReviewService.undo(ReviewContentType.JOB, 693L))
            .thenReturn(AdminReviewDecisionResult(ReviewContentType.JOB, 693L, ReviewStatus.PENDING, 3))

        mockMvc.perform(admin(decide("job", mapOf("decision" to "REJECTED", "reason" to "급여 조건이 비어 있습니다."))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.reviewStatus").value("REJECTED"))

        mockMvc.perform(admin(patch("/api/v1/admin/review-queue/job/693/undo")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.reviewStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.remaining").value(3))
    }

    @Test
    fun `반려 보관 목록은 종류와 검색어로 좁히고 삭제된 콘텐츠도 표시한다`() {
        Mockito.`when`(adminRejectionService.getRejections(ReviewContentType.JOB, "급여", 0, 20)).thenReturn(
            ContentRejectionPageDto(listOf(rejection(contentExists = false)), page = 0, size = 20, totalElements = 1, totalPages = 1),
        )

        mockMvc.perform(admin(get("/api/v1/admin/rejections").param("type", "JOB").param("keyword", "급여")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(512))
            .andExpect(jsonPath("$.data.items[0].contentExists").value(false))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
    }

    @Test
    fun `반려 사유는 비울 수 없고 고치면 고친 기록을 돌려준다`() {
        mockMvc.perform(admin(updateReason(" ")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[reason] 반려 사유를 입력해 주세요."))
        Mockito.verifyNoInteractions(adminRejectionService)

        Mockito.`when`(adminRejectionService.getRejection(ReviewContentType.JOB, 512L))
            .thenReturn(rejection(reason = "고친 사유"))

        mockMvc.perform(admin(updateReason("고친 사유")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.reason").value("고친 사유"))
        Mockito.verify(adminRejectionService).replaceReason(ReviewContentType.JOB, 512L, "고친 사유")
    }

    private fun decide(type: String, body: Map<String, String>): MockHttpServletRequestBuilder =
        patch("/api/v1/admin/review-queue/$type/${if (type == "bootcamp") 3 else 693}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))

    private fun updateReason(reason: String): MockHttpServletRequestBuilder =
        patch("/api/v1/admin/rejections/job/512")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapOf("reason" to reason)))

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("admin").authorities(SimpleGrantedAuthority(ADMIN_AUTHORITY)))

    private fun rejection(
        reason: String = "급여 조건이 비어 있습니다.",
        contentExists: Boolean = true,
    ): ContentRejectionDto = ContentRejectionDto(
        contentType = ReviewContentType.JOB,
        contentId = 512L,
        title = "Content Specialist",
        companyName = "뱅크",
        reason = reason,
        rejectedAt = LocalDateTime.of(2026, 9, 6, 11, 34),
        reasonUpdatedAt = null,
        contentExists = contentExists,
    )
}
