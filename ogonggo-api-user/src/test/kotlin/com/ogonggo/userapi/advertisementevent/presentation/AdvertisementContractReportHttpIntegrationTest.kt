package com.ogonggo.userapi.advertisementevent.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementevent.implement.AdvertisementContractReportProvider
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportMetricsDto
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementReportSummaryDto
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.advertisementevent.implement.AdvertisementReportAccess
import com.ogonggo.userapi.advertisementevent.implement.AdvertisementReportAccessReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate

/** 실제 내장 HTTP 서버의 인증·controller·service·응답 경로를 mock provider로 확인한다. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:advertisement-report-http;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
        "ogonggo.letscareer.base-url=http://localhost:8090",
        "ogonggo.letscareer.internal-api-key=test-internal-api-key",
        "spring.mail.host=localhost",
    ],
)
class AdvertisementContractReportHttpIntegrationTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val tokenProvider: OgonggoTokenProvider,
    private val objectMapper: ObjectMapper,
) {

    @LocalServerPort
    private var port: Int = 0

    @MockBean
    private lateinit var accessReader: AdvertisementReportAccessReader

    @MockBean
    private lateinit var reportProvider: AdvertisementContractReportProvider

    @Test
    fun `인증된 기업 회원 요청이 실제 report HTTP 경로를 지나 mock provider 응답을 반환한다`() {
        // given
        val today = LocalDate.now()
        val from = today.minusDays(1)
        val userId = 7101L
        val contractId = 8201L
        val exposureIds = listOf(9301L, 9302L)
        val report = AdvertisementContractReportDto(
            contractId = contractId,
            from = from,
            to = today,
            summary = AdvertisementReportSummaryDto(
                impressions = AdvertisementReportMetricsDto(total = 12, uniqueUsers = 8, perUser = 1.5),
                clicks = AdvertisementReportMetricsDto(total = 3, uniqueUsers = 2, perUser = 1.5),
            ),
            daily = emptyList(),
            positions = emptyList(),
            jobRoleDistribution = emptyList(),
        )
        Mockito.`when`(accessReader.read(userId, contractId))
            .thenReturn(AdvertisementReportAccess(userId, UserRole.COMPANY, exposureIds))
        Mockito.`when`(reportProvider.read(contractId, from, today, exposureIds)).thenReturn(report)

        val headers = HttpHeaders().apply {
            setBearerAuth(tokenProvider.createAccessToken(userId))
        }
        val url = "http://localhost:$port/api/v1/advertisement-contracts/$contractId/report?from=$from&to=$today"

        // when
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = objectMapper.readTree(checkNotNull(response.body))
        assertEquals(contractId, body["data"]["contractId"].asLong())
        assertEquals(12, body["data"]["summary"]["impressions"]["total"].asInt())
        assertEquals(3, body["data"]["summary"]["clicks"]["total"].asInt())
        assertNotNull(body["data"]["generatedAt"])
        Mockito.verify(reportProvider).read(contractId, from, today, exposureIds)
    }
}
