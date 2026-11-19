package com.ogonggo.userapi.advertisementevent.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractCompanyPostingAppendDto
import com.ogonggo.core.advertisementevent.implement.PosthogEndpointsClient
import com.ogonggo.core.advertisementevent.implement.PosthogEndpointsRequest
import com.ogonggo.core.advertisementevent.implement.PosthogEndpointsResponse
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureAppender
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureAppendDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductAppender
import com.ogonggo.core.advertisementproduct.implement.dto.AdvertisementProductSaveDto
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.dto.CompanyAccountAppendDto
import com.ogonggo.userapi.advertisementevent.implement.AdvertisementEventIdempotencyStore
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/** Real HTTP, auth, DB readers/writers, report provider and response mapping; only Redis and PostHog are replaced. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:advertisement-analytics-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
        "ogonggo.letscareer.base-url=http://localhost:8090",
        "ogonggo.letscareer.internal-api-key=test-internal-api-key",
        "spring.mail.host=localhost",
        "ogonggo.advertisement-report.source=POSTHOG",
        "ogonggo.posthog.endpoints.project-id=e2e-project",
        "ogonggo.posthog.endpoints.api-key=mock-endpoint-key",
    ],
)
class AdvertisementAnalyticsE2eTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val tokenProvider: OgonggoTokenProvider,
    private val userAppender: UserAppender,
    private val contractAppender: AdvertisementContractAppender,
    private val productAppender: AdvertisementProductAppender,
    private val exposureAppender: AdvertisementExposureAppender,
    private val transactionTemplate: TransactionTemplate,
    private val jdbcTemplate: JdbcTemplate,
) {

    @LocalServerPort
    private var port: Int = 0

    @MockBean
    private lateinit var eventIdempotencyStore: AdvertisementEventIdempotencyStore

    @MockBean
    private lateinit var posthogEndpointsClient: PosthogEndpointsClient

    @Test
    fun `노출·클릭 이벤트를 HTTP로 적재하고 계약 소유 리포트를 PostHog 응답으로 반환한다`() {
        // given
        val today = LocalDate.now(SEOUL)
        val owner = userAppender.appendCompany(
            CompanyAccountAppendDto("e2e-company@example.test", "encoded-password", LocalDateTime.now(SEOUL)),
        )
        val contractId = contractAppender.appendForCompanyPosting(
            AdvertisementContractCompanyPostingAppendDto(
                userId = owner.userId,
                promotionTarget = AdvertisementContractPromotionTarget.JOB,
                targetId = JOB_ID,
                companyName = "테스트 기업",
                recruitmentTitle = "테스트 공고",
                recruitmentDeadline = null,
            ),
        )
        val productId = productAppender.append(
            AdvertisementProductSaveDto(
                category = AdvertisementProductCategory.BANNER,
                position = AdvertisementProductPosition.JOB_HOME_TOP_BANNER,
                name = "홈 상단 배너",
                price = null,
            ),
        )
        val exposureId = checkNotNull(
            transactionTemplate.execute {
                exposureAppender.append(
                    AdvertisementExposureAppendDto(
                        productId = productId,
                        contractId = contractId,
                        content = AdvertisementExposureContent(
                            name = "테스트 배너",
                            imageUrl = "https://example.test/banner.png",
                            linkUrl = "https://example.test/job",
                        ),
                        adLabel = true,
                        period = AdvertisementExposurePeriod.ALWAYS,
                        exposed = true,
                    ),
                )
            },
        )
        val endpointRequest = PosthogEndpointsRequest(
            variables = com.ogonggo.core.advertisementevent.implement.PosthogAdvertisementReportVariables(
                from = today.toString(),
                to = today.toString(),
                exposureIds = exposureId.toString(),
            ),
        )
        Mockito.doAnswer { posthogFixture(today) }
            .`when`(posthogEndpointsClient)
            .run(Mockito.any(PosthogEndpointsRequest::class.java) ?: endpointRequest)
        val bearerToken = tokenProvider.createAccessToken(owner.userId)

        // when
        postEvent(exposureId, "IMPRESSION", bearerToken, UUID.randomUUID())
        postEvent(exposureId, "IMPRESSION", bearerToken, UUID.randomUUID())
        postEvent(exposureId, "CLICK", bearerToken, UUID.randomUUID())
        postEvent(exposureId, "CLICK", bearerToken, UUID.randomUUID())
        val reportResponse = restTemplate.exchange(
            "http://localhost:$port/api/v1/advertisement-contracts/$contractId/report?from=$today&to=$today",
            HttpMethod.GET,
            HttpEntity<Void>(headers(bearerToken)),
            String::class.java,
        )

        // then
        assertEquals(HttpStatus.OK, reportResponse.statusCode)
        val reportBody = objectMapper.readTree(checkNotNull(reportResponse.body))["data"]
        assertEquals(contractId, reportBody["contractId"].asLong())
        assertEquals(20, reportBody["summary"]["impressions"]["total"].asInt())
        assertEquals(6, reportBody["summary"]["clicks"]["total"].asInt())
        assertEquals(3L, countStoredEvents(exposureId))

        val requestCaptor = ArgumentCaptor.forClass(PosthogEndpointsRequest::class.java)
        Mockito.verify(posthogEndpointsClient).run(requestCaptor.capture() ?: endpointRequest)
        assertEquals(exposureId.toString(), requestCaptor.value.variables.exposureIds)
    }

    private fun postEvent(exposureId: Long, eventType: String, bearerToken: String, key: UUID) {
        val headers = headers(bearerToken).apply {
            contentType = MediaType.APPLICATION_JSON
            set("Idempotency-Key", key.toString())
        }
        val body = objectMapper.writeValueAsString(
            mapOf("exposureId" to exposureId, "eventType" to eventType, "anonymousId" to "browser-e2e"),
        )
        val response = restTemplate.exchange(
            "http://localhost:$port/api/v1/advertisement-events",
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode, response.body)
    }

    private fun headers(bearerToken: String) = HttpHeaders().apply { setBearerAuth(bearerToken) }

    private fun countStoredEvents(exposureId: Long): Long = checkNotNull(
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM advertisement_events WHERE exposure_id = ?",
            Long::class.java,
            exposureId,
        ),
    )

    private fun posthogFixture(reportDate: LocalDate): PosthogEndpointsResponse {
        val root = objectMapper.readTree(ClassPathResource("fixtures/posthog-advertisement-report-v1.json").inputStream)
        val rows = root["results"].map { row ->
            row.mapIndexed { index, value ->
                if (index == 1 && row[0].asText() == "DAILY") objectMapper.valueToTree(reportDate.toString()) else value
            }
        }
        return PosthogEndpointsResponse(
            results = rows,
            columns = root["columns"].map { it.asText() },
            executionId = root["execution_id"].asText(),
        )
    }

    private companion object {
        const val JOB_ID = 918_273L
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
