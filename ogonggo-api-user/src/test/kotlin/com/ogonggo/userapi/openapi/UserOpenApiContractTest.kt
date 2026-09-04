package com.ogonggo.userapi.openapi

import com.fasterxml.jackson.databind.JsonNode
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
        "spring.datasource.url=jdbc:h2:mem:ogonggo-user-openapi;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true",
        "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
        "ogonggo.letscareer.base-url=http://localhost:8090",
        "ogonggo.letscareer.internal-api-key=test-internal-api-key",
        // spring.mail.host가 있어야 Spring Boot가 JavaMailSender를 만든다. 실제로 발송하지는 않는다.
        "spring.mail.host=localhost",
        "ogonggo.advertisement.slack.inquiry-url=https://hooks.slack.com/services/T000/B000/test",
    ],
)
@AutoConfigureMockMvc
class UserOpenApiContractTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @Test
    fun `사용자 OpenAPI는 인터페이스의 경로와 인증과 오류 명세를 노출한다`() {
        val document = openApiDocument()

        assertEquals("Ogonggo User API", document.at("/info/title").asText())
        // 절대 URL이면 HTTP ALB 주소가 새어 나가 HTTPS Swagger UI에서 Try it out이 막힌다.
        assertEquals("/", document.at("/servers/0/url").asText())
        assertTrue(document.at("/components/securitySchemes/BearerAuth").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1jobs/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1jobs~1calendar/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1job-bookmarks/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}/post").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}/delete").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1bootcamps/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1auth~1letscareer/post").isObject)
        assertFalse(document.at("/paths/~1health").isObject)

        // 목록·상세는 로그인 없이 열려 있지만, 토큰을 보내면 북마크 여부가 채워지므로 스키마는 그대로 노출한다.
        val jobList = document.at("/paths/~1api~1v1~1jobs/get")
        assertTrue(jobList.at("/security/0/BearerAuth").isArray)
        assertPageParameter(jobList, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(jobList, "size", defaultValue = "10", minimum = 1, maximum = 100)

        // 부트캠프 조회와 공고 달력은 토큰을 전혀 쓰지 않으므로 Security Requirement를 붙이지 않는다.
        assertFalse(document.at("/paths/~1api~1v1~1bootcamps/get").has("security"))
        assertFalse(document.at("/paths/~1api~1v1~1bootcamps~1{bootcampId}/get").has("security"))

        val sourceUrlClick = document.at("/paths/~1api~1v1~1jobs~1{jobId}~1source-url-clicks/post")
        assertTrue(sourceUrlClick.at("/security/0/BearerAuth").isArray)

        val jobBookmarks = document.at("/paths/~1api~1v1~1job-bookmarks/get")
        assertTrue(jobBookmarks.at("/security/0/BearerAuth").isArray)
        assertPageParameter(jobBookmarks, "page", defaultValue = "1", minimum = 1, maximum = null)
        val addBookmark = document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}/post")
        assertTrue(addBookmark.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(addBookmark.at("/responses/409/description").asText().startsWith("JOB_BOOKMARK_ALREADY_EXISTS"))
        val deleteBookmark = document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}/delete")
        assertTrue(deleteBookmark.at("/responses/200/content/application~1json/schema").isObject)

        val jobCalendar = document.at("/paths/~1api~1v1~1jobs~1calendar/get")
        assertFalse(jobCalendar.has("security"))
        assertEquals("date", jobCalendar.parameter("from").at("/schema/format").asText())
        assertEquals("date", jobCalendar.parameter("to").at("/schema/format").asText())

        val calendarBadRequest = document.at("/paths/~1api~1v1~1jobs~1calendar/get/responses/400")
        assertTrue(calendarBadRequest["description"].asText().startsWith("BAD_REQUEST"))
        assertTrue(calendarBadRequest.at("/content/application~1json/schema").isObject)

        val jobNotFound = document.at("/paths/~1api~1v1~1jobs~1{jobId}/get/responses/404")
        assertTrue(jobNotFound["description"].asText().startsWith("JOB_NOT_FOUND"))
        assertTrue(jobNotFound.at("/content/application~1json/schema").isObject)

        val jobDetailProperties = document.at("/components/schemas/UserJobDetailResponse/properties")
        listOf(
            "companyAndTeamIntroduction",
            "responsibilities",
            "qualifications",
            "preferredQualifications",
            "compensation",
            "benefits",
            "hiringProcess",
        ).forEach { field -> assertTrue(jobDetailProperties.has(field)) }
        assertFalse(jobDetailProperties.has("content"))
        assertTrue(jobDetailProperties.has("bookmarked"))
        assertTrue(document.at("/components/schemas/UserJobSummaryResponse/properties/bookmarked").isObject)

        val companySignUp = document.at("/paths/~1api~1v1~1auth~1company~1signup/post")
        assertFalse(companySignUp.has("security"))
        assertTrue(companySignUp.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(companySignUp.at("/responses/409/description").asText().startsWith("EMAIL_ALREADY_EXISTS"))

        val companySignIn = document.at("/paths/~1api~1v1~1auth~1company~1signin/post")
        assertFalse(companySignIn.has("security"))
        assertTrue(
            companySignIn.at("/responses/401/description").asText().startsWith("INVALID_COMPANY_CREDENTIALS"),
        )

        // 광고 문의는 오공고 계정이 없는 기업 담당자가 호출하므로 Security Requirement를 붙이지 않는다.
        val advertisementInquiry = document.at("/paths/~1api~1v1~1advertisement-inquiries/post")
        assertTrue(advertisementInquiry.isObject)
        assertFalse(advertisementInquiry.has("security"))
        assertTrue(advertisementInquiry.at("/responses/200/content/application~1json/schema").isObject)
        assertTrue(
            advertisementInquiry.at("/responses/503/description").asText()
                .startsWith("ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED"),
        )

        val publicSignIn = document.at("/paths/~1api~1v1~1auth~1letscareer/post")
        assertFalse(publicSignIn.has("security"))
        val signOut = document.at("/paths/~1api~1v1~1auth~1signout/post")
        assertTrue(signOut.at("/security/0/BearerAuth").isArray)
    }

    private fun openApiDocument(): JsonNode {
        val response = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn()
            .response
        return objectMapper.readTree(response.contentAsString)
    }

    private fun assertPageParameter(
        operation: JsonNode,
        name: String,
        defaultValue: String,
        minimum: Int,
        maximum: Int?,
    ) {
        val parameter = operation["parameters"].first { it["name"].asText() == name }
        val schema = parameter["schema"]
        assertEquals(defaultValue, schema["default"].asText())
        assertEquals(minimum, schema["minimum"].asInt())
        if (maximum != null) {
            assertEquals(maximum, schema["maximum"].asInt())
        }
    }

    private fun JsonNode.parameter(name: String): JsonNode =
        this["parameters"].first { it["name"].asText() == name }
}
