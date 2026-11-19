package com.ogonggo.adminapi.advertisementreport.presentation

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
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/** Exercises the admin JSON and CSV HTTP routes with persisted contract/exposure data; PostHog is the external boundary. */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:admin-advertisement-report-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ogonggo.auth.jwt.secret=$JWT_SECRET",
        "ogonggo.admin.internal.api-key=test-internal-api-key",
        "ogonggo.advertisement-report.source=POSTHOG",
        "ogonggo.posthog.endpoints.project-id=e2e-project",
        "ogonggo.posthog.endpoints.api-key=mock-endpoint-key",
    ],
)
@AutoConfigureMockMvc
class AdminAdvertisementContractReportE2eTest @Autowired constructor(
    private val mvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val userAppender: UserAppender,
    private val contractAppender: AdvertisementContractAppender,
    private val productAppender: AdvertisementProductAppender,
    private val exposureAppender: AdvertisementExposureAppender,
    private val transactionTemplate: TransactionTemplate,
    private val jdbcTemplate: JdbcTemplate,
) {

    @MockBean
    private lateinit var posthogEndpointsClient: PosthogEndpointsClient

    @Test
    fun `관리자 JSON과 CSV 리포트가 계약 allowlist와 PostHog 집계를 사용한다`() {
        val today = LocalDate.now(SEOUL)
        val company = transactionTemplate.execute {
            userAppender.appendCompany(
                CompanyAccountAppendDto("admin-report-company@example.test", "encoded-password", LocalDateTime.now(SEOUL)),
            )
        }!!
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", company.userId)
        val contractId = contractAppender.appendForCompanyPosting(
            AdvertisementContractCompanyPostingAppendDto(
                userId = company.userId,
                promotionTarget = AdvertisementContractPromotionTarget.JOB,
                targetId = JOB_ID,
                companyName = "리포트 테스트 기업",
                recruitmentTitle = "E2E 테스트 공고",
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
        val accessToken = adminAccessToken(company.userId)
        val path = "/api/v1/admin/advertisement-contracts/$contractId"

        mvc.perform(
            get("$path/report")
                .queryParam("from", today.toString())
                .queryParam("to", today.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.contractId").value(contractId))
            .andExpect(jsonPath("$.data.summary.impressions.total").value(20))
            .andExpect(jsonPath("$.data.summary.clicks.total").value(6))
            .andExpect(jsonPath("$.data.jobRoleDistribution[0].key").value("IT_BACKEND"))

        val csv = mvc.perform(
            get("$path/report.csv")
                .queryParam("from", today.toString())
                .queryParam("to", today.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("text/csv;charset=UTF-8"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("advertisement-report-$contractId.csv")))
            .andReturn().response.contentAsString

        assertTrue(csv.contains("노출,20,10,2.0"))
        assertTrue(csv.contains("클릭 회원 직무"))
        val requestCaptor = org.mockito.ArgumentCaptor.forClass(PosthogEndpointsRequest::class.java)
        Mockito.verify(posthogEndpointsClient, Mockito.times(2)).run(requestCaptor.capture() ?: endpointRequest)
        assertEquals(exposureId.toString(), requestCaptor.allValues.last().variables.exposureIds)
    }

    private fun posthogFixture(reportDate: LocalDate): PosthogEndpointsResponse {
        val fixture = objectMapper.readValue(
            ClassPathResource("fixtures/posthog-advertisement-report-v1.json").inputStream,
            PosthogEndpointsResponse::class.java,
        )
        val rows = fixture.results.map { row ->
            row.mapIndexed { index, value ->
                if (index == 1 && row[0].asText() == "DAILY") objectMapper.valueToTree(reportDate.toString()) else value
            }
        }
        return fixture.copy(results = rows)
    }

    private fun adminAccessToken(userId: Long): String {
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET))
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "access")
            .issuedAt(Date(now))
            .expiration(Date(now + 600_000))
            .signWith(key, Jwts.SIG.HS512)
            .compact()
    }

    private companion object {
        const val JOB_ID = 918_274L
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}

private const val JWT_SECRET =
    "b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA=="
