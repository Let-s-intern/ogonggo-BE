package com.ogonggo.adminapi.job.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.InternalApiKeyAuthenticationFilter.Companion.INTERNAL_API_KEY_HEADER
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.job.business.CrawlerJobAnalysisCommand
import com.ogonggo.adminapi.job.business.CrawlerJobAnalysisService
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.error.JobErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [CrawlerJobAnalysisController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
@TestPropertySource(properties = ["ogonggo.admin.internal.api-key=$ANALYSIS_API_KEY"])
class CrawlerJobAnalysisControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var crawlerJobAnalysisService: CrawlerJobAnalysisService

    /** 관리자 콘솔 인증 필터가 쓰는 대역이다. 내부 경로는 이 필터를 거치지 않는다. */
    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @Test
    fun `분석 대상을 본문 해시와 함께 돌려준다`() {
        // given
        val job = job()
        Mockito.`when`(crawlerJobAnalysisService.getTargets(50)).thenReturn(listOf(job))

        // when & then
        mockMvc.perform(get("/api/v1/internal/jobs/analysis-targets").header(INTERNAL_API_KEY_HEADER, ANALYSIS_API_KEY))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].jobId").value(12))
            .andExpect(jsonPath("$.data[0].source").value("WORK24"))
            .andExpect(jsonPath("$.data[0].contentHash").value("hash"))
            .andExpect(jsonPath("$.data[0].qualifications").value("자격"))
            .andExpect(jsonPath("$.data[0].recruitmentNotice").isEmpty)
    }

    @Test
    fun `내부 API 키가 없으면 401로 응답한다`() {
        mockMvc.perform(get("/api/v1/internal/jobs/analysis-targets"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `대상 개수가 범위를 넘으면 400으로 응답한다`() {
        mockMvc.perform(
            get("/api/v1/internal/jobs/analysis-targets")
                .param("size", "201")
                .header(INTERNAL_API_KEY_HEADER, ANALYSIS_API_KEY),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `분석을 저장하고 빈 칸은 명시 없음으로 옮긴다`() {
        // given
        var saved: Pair<Long, CrawlerJobAnalysisCommand>? = null
        Mockito.doAnswer {
            saved = it.arguments[0] as Long to it.arguments[1] as CrawlerJobAnalysisCommand
            null
        }.`when`(crawlerJobAnalysisService).saveAnalysis(Mockito.anyLong(), anyCommand())

        // when
        mockMvc.perform(putAnalysis(12, requestBody()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        // then
        val (jobId, command) = checkNotNull(saved)
        assertEquals(12L, jobId)
        assertEquals("hash", command.contentHash)
        assertEquals(3, command.guideVersion)
        assertEquals("정규직", command.content.employment.type.value)
        assertNull(command.content.employment.salary.value)
        assertEquals("버그를 끝까지 추적한 경험", command.content.competencies.single().experiences.single())
    }

    @Test
    fun `역량이 세 개를 넘으면 저장하지 않고 400으로 응답한다`() {
        val competency = (requestBody()["analysis"] as Map<*, *>)["competencies"] as List<*>
        val tooMany = requestBody().withAnalysis("competencies" to List(4) { competency.single() })

        mockMvc.perform(putAnalysis(12, tooMany))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        Mockito.verifyNoInteractions(crawlerJobAnalysisService)
    }

    @Test
    fun `본문이 바뀐 공고의 분석은 409로 응답한다`() {
        Mockito.doThrow(ConflictException(JobErrorCode.JOB_ANALYSIS_OUTDATED))
            .`when`(crawlerJobAnalysisService).saveAnalysis(Mockito.anyLong(), anyCommand())

        mockMvc.perform(putAnalysis(12, requestBody()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("JOB_ANALYSIS_OUTDATED"))
    }

    private fun putAnalysis(jobId: Long, body: Map<String, Any?>) =
        put("/api/v1/internal/jobs/$jobId/analysis")
            .header(INTERNAL_API_KEY_HEADER, ANALYSIS_API_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))

    private fun requestBody(): Map<String, Any?> {
        val empty = mapOf("value" to null, "note" to null)
        return mapOf(
            "contentHash" to "hash",
            "guideVersion" to 3,
            "model" to "deepseek-flash",
            "analysis" to mapOf(
                "tasks" to listOf(mapOf("tag" to "개발", "text" to "서버를 개발해요.")),
                "required" to listOf("Kotlin 경험이 있는 분"),
                "preferred" to emptyList<String>(),
                "employment" to mapOf(
                    "type" to mapOf("value" to "정규직", "note" to ""),
                    "conversion" to empty,
                    "salary" to mapOf("value" to " ", "note" to null),
                    "affiliation" to empty,
                ),
                "submission" to mapOf("documents" to empty, "essay" to empty, "process" to empty, "deadline" to empty),
                "competencies" to listOf(
                    mapOf(
                        "name" to "문제 해결",
                        "quote" to "서버 개발",
                        "description" to "막힌 문제를 끝까지 푸는 역량이에요.",
                        "experiences" to listOf("버그를 끝까지 추적한 경험"),
                    ),
                ),
            ),
        )
    }

    private fun Map<String, Any?>.withAnalysis(entry: Pair<String, Any?>): Map<String, Any?> =
        this + ("analysis" to (this["analysis"] as Map<*, *>) + entry)

    private fun job(): Job = Mockito.mock(Job::class.java).also {
        Mockito.`when`(it.id).thenReturn(12L)
        Mockito.`when`(it.source).thenReturn(ContentSource.WORK24)
        Mockito.`when`(it.contentHash()).thenReturn("hash")
        Mockito.`when`(it.companyName).thenReturn("오공고")
        Mockito.`when`(it.title).thenReturn("사무보조")
        Mockito.`when`(it.employmentType).thenReturn(JobEmploymentType.FULL_TIME)
        Mockito.`when`(it.recruitmentType).thenReturn(JobRecruitmentType.ALWAYS_OPEN)
        Mockito.`when`(it.qualifications).thenReturn("자격")
    }
}

private const val ANALYSIS_API_KEY = "test-internal-api-key"

/** Mockito의 any()는 null을 반환해 Kotlin의 non-null 파라미터에 그대로 넘길 수 없다. */
@Suppress("UNCHECKED_CAST")
private fun <T> anyCommand(): T {
    Mockito.any<T>()
    return null as T
}
