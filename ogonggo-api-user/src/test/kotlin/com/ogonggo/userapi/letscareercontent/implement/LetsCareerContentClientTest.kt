package com.ogonggo.userapi.letscareercontent.implement

import com.ogonggo.core.letscareercontent.domain.LetsCareerContentKind
import com.ogonggo.userapi.auth.implement.LetsCareerProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

class LetsCareerContentClientTest {

    private val builder = RestClient.builder().baseUrl(BASE_URL)
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client = LetsCareerContentClient(builder.build(), LetsCareerProperties(BASE_URL, API_KEY))

    @Test
    fun `프로그램 유형과 갈래로 종류를 정하고 모르는 종류나 경로가 없는 항목은 뺀다`() {
        // given
        server.expect(requestTo("$BASE_URL/api/v1/internal/catalog"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-Internal-Api-Key", API_KEY))
            .andRespond(
                withSuccess(
                    """
                    {"status":200,"message":"ok","data":{"contentList":[
                      {"contentType":"PROGRAM","programType":"CHALLENGE","category":"MARKETING","contentId":10,"title":"마케팅 챌린지",
                       "path":"/program/challenge/10","labels":["마케팅"],"beginning":"2026-10-01T00:00:00","deadline":"2026-10-20T23:59:59"},
                      {"contentType":"MATERIAL","category":"MATERIAL","contentId":57,"title":"마케팅 자료집","path":"/library/57/x","labels":[]},
                      {"contentType":"BLOG","category":"JOB_PREPARATION_TIPS","contentId":248,"title":"자소서 팁","path":"/blog/248"},
                      {"contentType":"PROGRAM","programType":"LIVE_MENTORING","contentId":1,"title":"1:1 멘토링","path":"/x"},
                      {"contentType":"BLOG","contentId":249,"title":"경로 없음","path":null}
                    ]}}
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        // when
        val contents = client.readCatalog()

        // then
        server.verify()
        assertEquals(
            listOf(
                LetsCareerContentKind.CHALLENGE to 10L,
                LetsCareerContentKind.MATERIAL to 57L,
                LetsCareerContentKind.BLOG to 248L,
            ),
            contents.map { it.kind to it.externalId },
        )
        assertEquals(listOf("마케팅"), contents.first().source.labels)
    }

    @Test
    fun `렛츠커리어가 실패하면 빈 목록 대신 예외를 던진다`() {
        server.expect(requestTo("$BASE_URL/api/v1/internal/catalog"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows<RestClientException> { client.readCatalog() }
    }

    companion object {
        private const val BASE_URL = "http://letscareer.test"
        private const val API_KEY = "internal-key"
    }
}
