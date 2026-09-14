package com.ogonggo.adminapi.openapi

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ogonggo-admin-openapi;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true",
    ],
)
@AutoConfigureMockMvc
class AdminOpenApiContractTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    /** 사용자 API와 같은 이유로 operationId를 코드에서 정하고, 겹치거나 자동 접미사가 붙으면 빌드를 깨뜨린다. */
    @Test
    fun `모든 operationId는 유일하며 자동 생성 접미사가 붙지 않는다`() {
        val document = openApiDocument()

        val operationIds = document.at("/paths").fields().asSequence()
            .flatMap { (path, operations) ->
                operations.fields().asSequence().map { (method, operation) ->
                    "$method $path" to operation.at("/operationId").asText()
                }
            }
            .toList()

        assertTrue(operationIds.none { (_, id) -> id.isEmpty() }, "operationId가 없는 작업: $operationIds")
        assertTrue(operationIds.none { (_, id) -> id.matches(Regex(".*_\\d+$")) }, "접미사가 붙은 작업: $operationIds")
        assertTrue(operationIds.groupBy { it.second }.all { it.value.size == 1 }, "operationId가 겹칩니다: $operationIds")
    }

    @Test
    fun `관리자 콘솔 API는 Bearer 인증을 요구하고 목록 기본 크기는 20이다`() {
        val document = openApiDocument()

        val jobs = document.at("/paths/~1api~1v1~1admin~1jobs/get")
        assertTrue(jobs.at("/security/0/BearerAuth").isArray)
        assertEquals(
            "20",
            jobs.at("/parameters").first { it.at("/name").asText() == "size" }.at("/schema/default").asText(),
        )
        listOf(
            "/paths/~1api~1v1~1admin~1jobs~1{jobId}/patch",
            "/paths/~1api~1v1~1admin~1jobs~1{jobId}/delete",
            "/paths/~1api~1v1~1admin~1bootcamps/get",
            "/paths/~1api~1v1~1admin~1bootcamps~1{bootcampId}/patch",
            "/paths/~1api~1v1~1admin~1review-queue/get",
            "/paths/~1api~1v1~1admin~1review-queue~1{type}~1{id}/patch",
            "/paths/~1api~1v1~1admin~1review-queue~1{type}~1{id}~1undo/patch",
            "/paths/~1api~1v1~1admin~1rejections/get",
            "/paths/~1api~1v1~1admin~1rejections~1{type}~1{id}/patch",
        ).forEach { pointer ->
            assertTrue(document.at("$pointer/security/0/BearerAuth").isArray, "인증 명세가 없습니다: $pointer")
        }
    }

    private fun openApiDocument() = objectMapper.readTree(
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString,
    )

    @Test
    fun `관리자 OpenAPI는 독립된 정보와 인증 스키마를 제공하고 health를 제외한다`() {
        val response = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn()
            .response
        val document = objectMapper.readTree(response.contentAsString)

        assertEquals("Ogonggo Admin API", document.at("/info/title").asText())
        // 절대 URL이면 HTTP ALB 주소가 새어 나가 HTTPS Swagger UI에서 Try it out이 막힌다.
        assertEquals("/", document.at("/servers/0/url").asText())
        assertTrue(document.at("/components/securitySchemes/BearerAuth").isObject)
        assertFalse(document.at("/paths/~1health").isObject)
    }
}
