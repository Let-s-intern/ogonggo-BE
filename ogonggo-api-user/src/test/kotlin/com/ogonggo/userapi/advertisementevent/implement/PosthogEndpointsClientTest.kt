package com.ogonggo.userapi.advertisementevent.implement

import com.ogonggo.core.advertisementevent.error.AdvertisementEventErrorCode
import com.ogonggo.core.advertisementevent.implement.PosthogAdvertisementReportVariables
import com.ogonggo.core.advertisementevent.implement.PosthogEndpointsClient
import com.ogonggo.core.advertisementevent.implement.PosthogEndpointsProperties
import com.ogonggo.core.advertisementevent.implement.PosthogEndpointsRequest
import com.ogonggo.core.error.InternalServerException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference

class PosthogEndpointsClientTest {

    @Test
    @DisplayName("공식 Endpoints body와 version query parameter를 보내고 성공 응답을 캐시한다")
    fun `공식 호출 형식으로 보내고 version별 성공 응답을 캐시한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = PosthogEndpointsClient(builder.build(), configuredProperties(), MutableClock(NOW))
        val request = request()
        val body = """{"results":[],"columns":["record_type","event_date","category","position","event_type","total","unique_users","job_role_code","unique_clickers"],"execution_id":"test-run"}"""
        server.expect(requestTo("https://posthog.test/api/environments/123/endpoints/advertisement-contract-report/run?version=7"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer secret-test-key"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""{"variables":{"from":"2026-11-01","to":"2026-11-19","exposure_ids":"11,12"},"limit":1000,"offset":0}"""))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        val result = client.run(request)
        val cached = client.run(request)

        assertEquals(listOf("record_type", "event_date", "category", "position", "event_type", "total", "unique_users", "job_role_code", "unique_clickers"), result.columns)
        assertEquals(result, cached)
        server.verify()
    }

    @Test
    @DisplayName("결과 limit과 offset을 body로 보내 페이지를 이어 붙인다")
    fun `공식 pagination body로 모든 페이지를 합친다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val properties = configuredProperties().apply { pageSize = 2; maxRows = 4 }
        val client = PosthogEndpointsClient(builder.build(), properties, MutableClock(NOW))
        val url = "https://posthog.test/api/environments/123/endpoints/advertisement-contract-report/run?version=7"
        val columns = "[\"record_type\",\"event_date\",\"category\",\"position\",\"event_type\",\"total\",\"unique_users\",\"job_role_code\",\"unique_clickers\"]"
        val firstPage = """{"results":[["SUMMARY",null,null,null,"IMPRESSION",2,2,null,null],["SUMMARY",null,null,null,"CLICK",1,1,null,null]],"columns":$columns}"""
        val secondPage = """{"results":[["POSITION",null,"JOB_POSTING","JOB_HOME_LIST",null,1,1,null,null]],"columns":$columns}"""
        server.expect(requestTo(url))
            .andExpect(content().json("""{"variables":{"from":"2026-11-01","to":"2026-11-19","exposure_ids":"11,12"},"limit":2,"offset":0}"""))
            .andRespond(withSuccess(firstPage, MediaType.APPLICATION_JSON))
        server.expect(requestTo(url))
            .andExpect(content().json("""{"variables":{"from":"2026-11-01","to":"2026-11-19","exposure_ids":"11,12"},"limit":2,"offset":2}"""))
            .andRespond(withSuccess(secondPage, MediaType.APPLICATION_JSON))

        val result = client.run(request())

        assertEquals(3, result.results.size)
        server.verify()
    }

    @Test
    @DisplayName("최대 행 상한을 넘는 다음 페이지는 503으로 실패한다")
    fun `결과가 최대 행 상한을 넘으면 안전하게 실패한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val properties = configuredProperties().apply { pageSize = 2; maxRows = 2 }
        val client = PosthogEndpointsClient(builder.build(), properties, MutableClock(NOW))
        val url = "https://posthog.test/api/environments/123/endpoints/advertisement-contract-report/run?version=7"
        val columns = "[\"record_type\",\"event_date\",\"category\",\"position\",\"event_type\",\"total\",\"unique_users\",\"job_role_code\",\"unique_clickers\"]"
        val firstPage = """{"results":[["SUMMARY",null,null,null,"IMPRESSION",2,2,null,null],["SUMMARY",null,null,null,"CLICK",1,1,null,null]],"columns":$columns}"""
        val secondPage = """{"results":[["POSITION",null,"JOB_POSTING","JOB_HOME_LIST",null,1,1,null,null]],"columns":$columns}"""
        server.expect(requestTo(url))
            .andExpect(content().json("""{"limit":2,"offset":0}"""))
            .andRespond(withSuccess(firstPage, MediaType.APPLICATION_JSON))
        server.expect(requestTo(url))
            .andExpect(content().json("""{"limit":2,"offset":2}"""))
            .andRespond(withSuccess(secondPage, MediaType.APPLICATION_JSON))

        val exception = assertThrows(InternalServerException::class.java) { client.run(request()) }

        assertEquals(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_SOURCE_UNAVAILABLE, exception.errorCode)
        server.verify()
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403, 429, 500, 502, 503])
    @DisplayName("인증·권한·rate limit·서버 오류는 안전한 503으로 매핑하고 응답 본문을 감춘다")
    fun `upstream 오류를 안전한 source unavailable로 매핑한다`(status: Int) {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = PosthogEndpointsClient(builder.build(), configuredProperties(), MutableClock(NOW))
        server.expect(requestTo("https://posthog.test/api/environments/123/endpoints/advertisement-contract-report/run?version=7"))
            .andRespond(withStatus(HttpStatus.valueOf(status)).body("private upstream detail secret-test-key"))

        val exception = assertThrows(InternalServerException::class.java) { client.run(request()) }

        assertEquals(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_SOURCE_UNAVAILABLE, exception.errorCode)
        assertFalse(exception.message.orEmpty().contains("secret-test-key"))
        assertFalse(exception.message.orEmpty().contains("private upstream"))
        server.verify()
    }

    @Test
    @DisplayName("timeout은 upstream 메시지와 secret을 노출하지 않고 503으로 매핑한다")
    fun `timeout은 안전한 503으로 매핑한다`() {
        val requestFactory = ClientHttpRequestFactory { _, _ -> throw ResourceAccessException("secret-bearing timeout detail") }
        val client = PosthogEndpointsClient(RestClient.builder().requestFactory(requestFactory).build(), configuredProperties(), MutableClock(NOW))

        val exception = assertThrows(InternalServerException::class.java) { client.run(request()) }

        assertEquals(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_SOURCE_UNAVAILABLE, exception.errorCode)
        assertFalse(exception.message.orEmpty().contains("secret-bearing"))
    }

    @Test
    @DisplayName("credential 누락은 요청하지 않고 properties 문자열은 키를 마스킹한다")
    fun `키가 없으면 호출하지 않고 설정문자열에서 키를 감춘다`() {
        val properties = configuredProperties().apply { apiKey = "" }
        val client = PosthogEndpointsClient(RestClient.builder().build(), properties, MutableClock(NOW))

        val exception = assertThrows(InternalServerException::class.java) { client.run(request()) }

        assertEquals(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_SOURCE_UNAVAILABLE, exception.errorCode)
        assertFalse(configuredProperties().toString().contains("secret-test-key"))
        assertFalse(configuredProperties().toString().contains("apiKey=secret"))
    }

    @Test
    @DisplayName("cache TTL 만료 뒤에는 upstream을 다시 호출한다")
    fun `cache TTL 뒤에는 다시 호출한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val clock = MutableClock(NOW)
        val client = PosthogEndpointsClient(builder.build(), configuredProperties(), clock)
        val url = "https://posthog.test/api/environments/123/endpoints/advertisement-contract-report/run?version=7"
        repeat(2) {
            server.expect(requestTo(url)).andRespond(withSuccess(emptyEnvelope(), MediaType.APPLICATION_JSON))
        }

        client.run(request())
        clock.advanceSeconds(61)
        client.run(request())

        server.verify()
    }

    private fun configuredProperties() = PosthogEndpointsProperties().apply {
        apiHost = "https://posthog.test"
        projectId = "123"
        endpointName = "advertisement-contract-report"
        apiKey = "secret-test-key"
        reportVersion = 7
    }

    private fun request() = PosthogEndpointsRequest(
        PosthogAdvertisementReportVariables("2026-11-01", "2026-11-19", "11,12"),
    )

    private fun emptyEnvelope() = """{"results":[],"columns":["record_type","event_date","category","position","event_type","total","unique_users","job_role_code","unique_clickers"]}"""

    private class MutableClock(initial: Instant) : Clock() {
        private val instant = AtomicReference(initial)
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = instant.get()
        fun advanceSeconds(seconds: Long) = instant.updateAndGet { it.plusSeconds(seconds) }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-11-19T03:00:00Z")
    }
}
