package com.ogonggo.userapi.advertisementevent.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ogonggo.core.advertisementevent.error.AdvertisementEventErrorCode
import com.ogonggo.core.advertisementevent.implement.PosthogAdvertisementContractReportProvider
import com.ogonggo.core.advertisementevent.implement.PosthogAdvertisementReportEndpointMapper
import com.ogonggo.core.advertisementevent.implement.PosthogAdvertisementReportVariables
import com.ogonggo.core.advertisementevent.implement.PosthogEndpointsClient
import com.ogonggo.core.advertisementevent.implement.PosthogEndpointsProperties
import com.ogonggo.core.advertisementevent.implement.PosthogEndpointsRequest
import com.ogonggo.core.advertisementevent.implement.PosthogEndpointsResponse
import com.ogonggo.core.error.InternalServerException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class PosthogAdvertisementContractReportProviderTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val mapper = PosthogAdvertisementReportEndpointMapper()

    @Test
    @DisplayName("version 1 fixture의 typed rows를 기존 리포트 DTO로 변환한다")
    fun `version 1 fixture를 리포트 DTO로 매핑한다`() {
        val response = fixture()

        val result = mapper.report(response, CONTRACT_ID, FROM, TO)

        assertEquals(20, result.summary.impressions.total)
        assertEquals(10, result.summary.impressions.uniqueUsers)
        assertEquals(2.0, result.summary.impressions.perUser)
        assertEquals(6, result.summary.clicks.total)
        assertEquals(3, result.summary.clicks.uniqueUsers)
        assertEquals("BANNER", result.daily.single().category.name)
        assertEquals("JOB_HOME_TOP_BANNER", result.daily.single().position?.name)
        assertEquals("IT_BACKEND", result.jobRoleDistribution[0].key)
        assertEquals("NON_MEMBER", result.jobRoleDistribution[1].key)
    }

    @Test
    @DisplayName("유효한 공통 envelope와 빈 rows는 0 리포트다")
    fun `빈 rows는 유효한 0 report다`() {
        val response = fixture().copy(results = emptyList())

        val result = mapper.report(response, CONTRACT_ID, FROM, TO)

        assertEquals(0, result.summary.impressions.total)
        assertEquals(0, result.summary.clicks.uniqueUsers)
        assertEquals(emptyList<Any>(), result.daily)
        assertEquals(emptyList<Any>(), result.positions)
        assertEquals(emptyList<Any>(), result.jobRoleDistribution)
    }

    @Test
    @DisplayName("provider는 계약 범위 allowlist를 정렬·중복 제거해 고정 variable로 보낸다")
    fun `provider는 서버 allowlist만 고정 변수로 보낸다`() {
        val client = Mockito.mock(PosthogEndpointsClient::class.java)
        val request = PosthogEndpointsRequest(PosthogAdvertisementReportVariables(FROM.toString(), TO.toString(), "11,12"))
        val response = fixture()
        Mockito.`when`(client.run(request)).thenReturn(response)
        val provider = PosthogAdvertisementContractReportProvider(client, mapper)

        val report = provider.read(CONTRACT_ID, FROM, TO, listOf(12, 11, 12))

        assertEquals(20, report.summary.impressions.total)
        Mockito.verify(client).run(request)
    }

    @Test
    @DisplayName("계약에 노출 ID가 없으면 외부 호출 없이 빈 리포트를 돌려준다")
    fun `allowlist가 비어 있으면 외부 호출 없이 빈 report를 돌려준다`() {
        val client = Mockito.mock(PosthogEndpointsClient::class.java)
        val provider = PosthogAdvertisementContractReportProvider(client, mapper)

        val report = provider.read(CONTRACT_ID, FROM, TO, emptyList())

        assertEquals(0, report.summary.impressions.total)
        Mockito.verifyNoInteractions(client)
    }

    @Test
    @DisplayName("PostHog HTTP mock 응답이 버전 지정 request부터 기존 DTO까지 이어진다")
    fun `mock HTTP endpoint의 성공 envelope가 provider report로 연결된다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val properties = PosthogEndpointsProperties().apply {
            apiHost = "https://posthog.test"
            projectId = "123"
            endpointName = "advertisement-contract-report"
            reportVersion = 1
            apiKey = "test-only-secret"
        }
        val client = PosthogEndpointsClient(builder.build(), properties, Clock.fixed(NOW, ZoneOffset.UTC))
        val provider = PosthogAdvertisementContractReportProvider(client, mapper)
        val payload = javaClass.getResource("/fixtures/posthog-advertisement-report-v1.json")!!.readText()
        server.expect(requestTo("https://posthog.test/api/environments/123/endpoints/advertisement-contract-report/run?version=1"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(org.springframework.http.HttpMethod.POST))
            .andExpect(content().json("""{"variables":{"from":"2026-11-01","to":"2026-11-19","exposure_ids":"11,12"},"limit":1000,"offset":0}"""))
            .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON))

        val report = provider.read(CONTRACT_ID, FROM, TO, listOf(11, 12))

        assertEquals(6, report.summary.clicks.total)
        server.verify()
    }

    @Test
    @DisplayName("열 누락·추가·타입 오류·행 폭 오류·누락 summary는 0으로 숨기지 않고 503이다")
    fun `비정상 endpoint schema는 안전한 upstream 오류다`() {
        val valid = fixture()
        val cases = listOf(
            valid.copy(columns = valid.columns.dropLast(1)),
            valid.copy(columns = valid.columns + "unexpected"),
            valid.copy(results = valid.results.mapIndexed { index, row -> if (index == 0) row.dropLast(1) else row }),
            valid.copy(results = valid.results.mapIndexed { index, row -> if (index == 0) row.toMutableList().also { it[0] = objectMapper.readTree("7") } else row }),
            valid.copy(results = valid.results.drop(1)),
        )

        cases.forEach { response ->
            val exception = assertThrows(InternalServerException::class.java) {
                mapper.report(response, CONTRACT_ID, FROM, TO)
            }
            assertEquals(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_SOURCE_UNAVAILABLE, exception.errorCode)
        }
    }

    @Test
    @DisplayName("버전 1 계약에 없는 enum과 기간 밖 날짜는 거부한다")
    fun `지원하지 않는 enum과 기간 밖 날짜는 거부한다`() {
        val valid = fixture()
        val invalidCategory = valid.copy(
            results = valid.results.mapIndexed { index, row -> if (index == 2) row.toMutableList().also { it[2] = objectMapper.readTree("\"UNSUPPORTED\"") } else row },
        )
        val outsideRange = valid.copy(
            results = valid.results.mapIndexed { index, row -> if (index == 2) row.toMutableList().also { it[1] = objectMapper.readTree("\"2026-10-31\"") } else row },
        )

        listOf(invalidCategory, outsideRange).forEach { response ->
            assertEquals(
                AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_SOURCE_UNAVAILABLE,
                assertThrows(InternalServerException::class.java) { mapper.report(response, CONTRACT_ID, FROM, TO) }.errorCode,
            )
        }
    }

    private fun fixture(): PosthogEndpointsResponse = objectMapper.readValue(
        javaClass.getResource("/fixtures/posthog-advertisement-report-v1.json")!!.readText(),
        PosthogEndpointsResponse::class.java,
    )

    private companion object {
        const val CONTRACT_ID = 42L
        val FROM: LocalDate = LocalDate.of(2026, 11, 1)
        val TO: LocalDate = LocalDate.of(2026, 11, 19)
        val NOW: Instant = Instant.parse("2026-11-19T03:00:00Z")
    }
}
