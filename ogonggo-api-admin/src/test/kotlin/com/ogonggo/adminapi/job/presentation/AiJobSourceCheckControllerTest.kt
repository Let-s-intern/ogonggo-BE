package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.InternalApiKeyAuthenticationFilter.Companion.INTERNAL_API_KEY_HEADER
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.job.business.AiJobSourceCheckService
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.error.JobErrorCode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AiJobSourceCheckController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
@TestPropertySource(properties = ["ogonggo.admin.internal.api-key=$SOURCE_CHECK_API_KEY"])
class AiJobSourceCheckControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var aiJobSourceCheckService: AiJobSourceCheckService

    /** 관리자 콘솔 인증 필터가 쓰는 대역이다. 내부 경로는 이 필터를 거치지 않는다. */
    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @Test
    fun `원문 확인 대상을 원문 URL과 함께 돌려준다`() {
        // given
        Mockito.`when`(aiJobSourceCheckService.getTargets(30, 100)).thenReturn(listOf(job()))

        // when & then
        mockMvc.perform(
            get("/api/v1/internal/jobs/source-check-targets")
                .param("afterId", "30")
                .header(INTERNAL_API_KEY_HEADER, SOURCE_CHECK_API_KEY),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].jobId").value(42))
            .andExpect(jsonPath("$.data[0].source").value("COMPANY"))
            .andExpect(jsonPath("$.data[0].sourceUrl").value("https://example.com/jobs/1"))
            .andExpect(jsonPath("$.data[0].title").value("백엔드 개발자"))
    }

    @Test
    fun `대상 개수가 범위를 넘으면 400으로 응답한다`() {
        mockMvc.perform(
            get("/api/v1/internal/jobs/source-check-targets")
                .param("size", "501")
                .header(INTERNAL_API_KEY_HEADER, SOURCE_CHECK_API_KEY),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `내부 API 키가 없으면 401로 응답한다`() {
        mockMvc.perform(post("/api/v1/internal/jobs/42/source-check-close"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `공고를 마감하고 빈 본문으로 응답한다`() {
        mockMvc.perform(post("/api/v1/internal/jobs/42/source-check-close").header(INTERNAL_API_KEY_HEADER, SOURCE_CHECK_API_KEY))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())
        Mockito.verify(aiJobSourceCheckService).close(42)
    }

    @Test
    fun `없는 공고를 마감하면 404로 응답한다`() {
        Mockito.doThrow(EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)).`when`(aiJobSourceCheckService).close(42)

        mockMvc.perform(post("/api/v1/internal/jobs/42/source-check-close").header(INTERNAL_API_KEY_HEADER, SOURCE_CHECK_API_KEY))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"))
    }

    private fun job(): Job = Mockito.mock(Job::class.java).also {
        Mockito.`when`(it.id).thenReturn(42L)
        Mockito.`when`(it.source).thenReturn(ContentSource.COMPANY)
        Mockito.`when`(it.sourceUrl).thenReturn("https://example.com/jobs/1")
        Mockito.`when`(it.companyName).thenReturn("오공고")
        Mockito.`when`(it.title).thenReturn("백엔드 개발자")
    }
}

private const val SOURCE_CHECK_API_KEY = "test-internal-api-key"
