package com.ogonggo.adminapi.job.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.internal.implement.InternalApiKeyAuthenticationFilter.Companion.INTERNAL_API_KEY_HEADER
import com.ogonggo.adminapi.job.business.CrawlerJobService
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.error.JobErrorCode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [CrawlerJobController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
@TestPropertySource(properties = ["ogonggo.admin.internal.api-key=$API_KEY"])
class CrawlerJobControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var crawlerJobService: CrawlerJobService

    @Test
    fun `내부 API 키가 있으면 공고를 등록하고 식별자를 반환한다`() {
        Mockito.`when`(crawlerJobService.register(anyCommand())).thenReturn(7L)

        mockMvc.perform(
            post("/api/v1/internal/jobs")
                .header(INTERNAL_API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalRequestBody())),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
            .andExpect(jsonPath("$.data.jobId").value(7))
    }

    @Test
    fun `내부 API 키가 없거나 다르면 401로 응답한다`() {
        mockMvc.perform(
            post("/api/v1/internal/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalRequestBody())),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc.perform(
            post("/api/v1/internal/jobs")
                .header(INTERNAL_API_KEY_HEADER, "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalRequestBody())),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `필수값이 없으면 400으로 응답한다`() {
        mockMvc.perform(
            post("/api/v1/internal/jobs")
                .header(INTERNAL_API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        minimalRequestBody() + mapOf("title" to " "),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("[title] 채용공고 제목은 필수입니다."))
    }

    @Test
    fun `이미 등록된 원문은 409로 응답한다`() {
        Mockito.`when`(crawlerJobService.register(anyCommand()))
            .thenThrow(ConflictException(JobErrorCode.JOB_ALREADY_EXISTS))

        mockMvc.perform(
            post("/api/v1/internal/jobs")
                .header(INTERNAL_API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalRequestBody())),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("JOB_ALREADY_EXISTS"))
    }

    private fun minimalRequestBody(): Map<String, Any> = mapOf(
        "companyName" to "오공고",
        "title" to "백엔드 개발자",
        "employmentType" to "FULL_TIME",
        "sourceUrl" to "https://example.com/jobs/1",
        "tags" to listOf("백엔드"),
    )

}

private const val API_KEY = "test-internal-api-key"

/**
 * Mockito의 any()는 null을 반환해 Kotlin의 non-null 파라미터에 그대로 넘길 수 없다.
 * 매처를 등록한 뒤 검사 없는 캐스트로 자리만 채운다.
 */
@Suppress("UNCHECKED_CAST")
private fun <T> anyCommand(): T {
    Mockito.any<T>()
    return null as T
}
