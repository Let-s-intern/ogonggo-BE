package com.ogonggo.adminapi.bootcamp.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.bootcamp.business.CrawlerBootcampCommand
import com.ogonggo.adminapi.bootcamp.business.CrawlerBootcampService
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.adminapi.internal.implement.InternalApiKeyAuthenticationFilter.Companion.INTERNAL_API_KEY_HEADER
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [CrawlerBootcampController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
@TestPropertySource(properties = ["ogonggo.admin.internal.api-key=$API_KEY"])
class CrawlerBootcampControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockBean
    private lateinit var crawlerBootcampService: CrawlerBootcampService

    /** 관리자 콘솔 인증 필터가 쓰는 대역이다. 내부 경로는 이 필터를 거치지 않는다. */
    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @Test
    fun `내부 API 키가 있으면 부트캠프를 등록하고 식별자를 반환한다`() {
        var registered: CrawlerBootcampCommand? = null
        Mockito.`when`(crawlerBootcampService.register(anyCommand())).thenAnswer { invocation ->
            registered = invocation.arguments[0] as CrawlerBootcampCommand
            11L
        }

        mockMvc.perform(
            post("/api/v1/internal/bootcamps")
                .header(INTERNAL_API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody())),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
            .andExpect(jsonPath("$.data.bootcampId").value(11))

        // 오프셋 없는 ISO 일시와 날짜를 그대로 읽는다.
        val command = checkNotNull(registered)
        assertEquals(LocalDateTime.of(2026, 9, 30, 23, 59, 59), command.recruitmentEndAt)
        assertEquals(LocalDate.of(2026, 10, 6), command.programStartDate)
        assertEquals(listOf("자바 기초", "스프링"), command.curriculums.map { it.subtitle })
    }

    @Test
    fun `내부 API 키가 없거나 다르면 401로 응답한다`() {
        mockMvc.perform(
            post("/api/v1/internal/bootcamps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody())),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

        mockMvc.perform(
            get("/api/v1/internal/bootcamps").param("sourceUrl", SOURCE_URL).header(INTERNAL_API_KEY_HEADER, "wrong-key"),
        ).andExpect(status().isUnauthorized)

        mockMvc.perform(delete("/api/v1/internal/bootcamps/11")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `필수값이 없으면 400으로 응답한다`() {
        mockMvc.perform(
            post("/api/v1/internal/bootcamps")
                .header(INTERNAL_API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody() + mapOf("title" to " "))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("[title] 프로그램명은 필수입니다."))

        listOf("sourceUrl", "operationType", "programStartDate").forEach { field ->
            mockMvc.perform(
                post("/api/v1/internal/bootcamps")
                    .header(INTERNAL_API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody() - field)),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        }
    }

    @Test
    fun `커리큘럼 한 줄이 잘못되면 어느 칸인지 400으로 알린다`() {
        mockMvc.perform(
            post("/api/v1/internal/bootcamps")
                .header(INTERNAL_API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        requestBody() + mapOf("curriculums" to listOf(mapOf("startWeek" to 0, "endWeek" to 2, "subtitle" to "기초"))),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[curriculums[0].startWeek] 시작 주차는 1 이상이어야 합니다."))
    }

    @Test
    fun `이메일 지원에 지원 페이지 주소를 보내면 어느 칸이 틀렸는지 알린다`() {
        mockMvc.perform(
            post("/api/v1/internal/bootcamps")
                .header(INTERNAL_API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody() + mapOf("applicationMethod" to "EMAIL"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("[applicationUrl] 이메일 지원에는 외부 지원 링크를 설정할 수 없습니다."))
    }

    @Test
    fun `이미 등록된 원문은 409로 응답한다`() {
        Mockito.`when`(crawlerBootcampService.register(anyCommand()))
            .thenThrow(ConflictException(BootcampErrorCode.BOOTCAMP_ALREADY_EXISTS))

        mockMvc.perform(
            post("/api/v1/internal/bootcamps")
                .header(INTERNAL_API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody())),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("BOOTCAMP_ALREADY_EXISTS"))
    }

    @Test
    fun `원문 URL로 수집 부트캠프의 식별자를 찾고 없으면 404로 응답한다`() {
        Mockito.`when`(crawlerBootcampService.getBootcampId(SOURCE_URL)).thenReturn(11L)
        Mockito.`when`(crawlerBootcampService.getBootcampId(OTHER_SOURCE_URL))
            .thenThrow(EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND))

        mockMvc.perform(
            get("/api/v1/internal/bootcamps").param("sourceUrl", SOURCE_URL).header(INTERNAL_API_KEY_HEADER, API_KEY),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.bootcampId").value(11))

        mockMvc.perform(
            get("/api/v1/internal/bootcamps").param("sourceUrl", OTHER_SOURCE_URL).header(INTERNAL_API_KEY_HEADER, API_KEY),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("BOOTCAMP_NOT_FOUND"))
    }

    @Test
    fun `부트캠프를 교체하고 데이터 없이 200으로 응답한다`() {
        mockMvc.perform(
            put("/api/v1/internal/bootcamps/11")
                .header(INTERNAL_API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody())),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(crawlerBootcampService).replace(Mockito.eq(11L), anyCommand())
    }

    @Test
    fun `식별자가 양수가 아니면 교체하지 않고 400으로 응답한다`() {
        mockMvc.perform(
            put("/api/v1/internal/bootcamps/0")
                .header(INTERNAL_API_KEY_HEADER, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody())),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    @Test
    fun `부트캠프를 삭제하고 데이터 없이 200으로 응답한다`() {
        mockMvc.perform(delete("/api/v1/internal/bootcamps/11").header(INTERNAL_API_KEY_HEADER, API_KEY))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        Mockito.verify(crawlerBootcampService).delete(11L)
    }

    private fun requestBody(): Map<String, Any> = mapOf(
        "companyName" to "오공고 교육사",
        "title" to "백엔드 부트캠프",
        "programType" to "개발",
        "operationType" to "ONLINE",
        "recruitmentType" to "PERIOD",
        "recruitmentStartAt" to "2026-09-01T00:00:00",
        "recruitmentEndAt" to "2026-09-30T23:59:59",
        "programStartDate" to "2026-10-06",
        "programEndDate" to "2027-01-30",
        "tuitionType" to "FREE",
        "representativeImageUrl" to "https://example.com/images/bootcamp.png",
        "shortDescription" to "백엔드 개발자로 성장하는 16주",
        "content" to "부트캠프 상세 내용",
        "applicationMethod" to "EXTERNAL_PAGE",
        "applicationUrl" to "https://example.com/apply",
        "sourceUrl" to SOURCE_URL,
        "curriculums" to listOf(
            mapOf("startWeek" to 1, "endWeek" to 4, "subtitle" to "자바 기초"),
            mapOf("startWeek" to 5, "endWeek" to 8, "subtitle" to "스프링"),
        ),
    )
}

private const val API_KEY = "test-internal-api-key"
private const val SOURCE_URL = "https://example.com/bootcamps/1"
private const val OTHER_SOURCE_URL = "https://example.com/bootcamps/2"

/**
 * Mockito의 any()는 null을 반환해 Kotlin의 non-null 파라미터에 그대로 넘길 수 없다.
 * 매처를 등록한 뒤 검사 없는 캐스트로 자리만 채운다.
 */
@Suppress("UNCHECKED_CAST")
private fun <T> anyCommand(): T {
    Mockito.any<T>()
    return null as T
}
