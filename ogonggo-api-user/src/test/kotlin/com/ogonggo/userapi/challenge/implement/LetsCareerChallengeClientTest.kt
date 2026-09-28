package com.ogonggo.userapi.challenge.implement

import com.ogonggo.userapi.auth.implement.LetsCareerProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
import java.time.LocalDateTime

class LetsCareerChallengeClientTest {

    private val builder = RestClient.builder().baseUrl(BASE_URL)
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client = LetsCareerChallengeClient(builder.build(), LetsCareerProperties(BASE_URL, API_KEY))

    @Test
    fun `렛츠커리어 사용자 id와 내부 API 키를 실어 추천 챌린지를 가져온다`() {
        // given
        server.expect(requestTo("$BASE_URL$PATH?userId=42"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-Internal-Api-Key", API_KEY))
            .andRespond(withSuccess(ONE_CHALLENGE_BODY, MediaType.APPLICATION_JSON))

        // when
        val challenges = client.readRecommended(42L)

        // then
        server.verify()
        assertEquals(1, challenges.size)
        with(challenges.single()) {
            assertEquals(10L, challengeId)
            assertEquals("자소서 챌린지", title)
            assertEquals("한 달 만에 자소서 완성", shortDescription)
            assertEquals(LocalDateTime.of(2026, 9, 20, 0, 0), recruitmentStartAt)
            assertEquals(LocalDateTime.of(2026, 10, 5, 23, 59, 59), recruitmentEndAt)
            assertEquals(LocalDateTime.of(2026, 10, 7, 19, 0), programStartAt)
        }
    }

    @Test
    fun `렛츠커리어 사용자를 모르면 userId 없이 요청한다`() {
        server.expect(requestTo("$BASE_URL$PATH"))
            .andRespond(withSuccess("""{"status":200,"message":"ok","data":{"challengeList":[]}}""", MediaType.APPLICATION_JSON))

        val challenges = client.readRecommended(null)

        server.verify()
        assertTrue(challenges.isEmpty())
    }

    @Test
    fun `렛츠커리어가 실패하면 오류 대신 빈 목록을 돌려준다`() {
        server.expect(requestTo("$BASE_URL$PATH"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertTrue(client.readRecommended(null).isEmpty())
    }

    @Test
    fun `식별자나 제목이 없는 항목은 빼고 나머지를 돌려준다`() {
        server.expect(requestTo("$BASE_URL$PATH"))
            .andRespond(
                withSuccess(
                    """
                    {"status":200,"message":"ok","data":{"challengeList":[
                      {"id":null,"title":"식별자 없음"},
                      {"id":11,"title":" "},
                      {"id":12,"title":"포트폴리오 챌린지"}
                    ]}}
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        assertEquals(listOf(12L), client.readRecommended(null).map { it.challengeId })
    }

    private companion object {
        const val BASE_URL = "http://letscareer.test"
        const val PATH = "/api/v1/internal/challenges/recommend"
        const val API_KEY = "internal-key"

        // 렛츠커리어 ChallengeRecommendVo가 실제로 싣는 필드를 모두 담아, 모르는 필드가 와도 읽히는지 함께 확인한다.
        val ONE_CHALLENGE_BODY = """
            {"status":200,"message":"요청이 성공했습니다.","data":{"challengeList":[{
              "id":10,"programType":"CHALLENGE","programStatusType":"PROCEEDING","challengeType":"PERSONAL_STATEMENT",
              "title":"자소서 챌린지","thumbnail":"https://cdn.test/10.png","shortDesc":"한 달 만에 자소서 완성",
              "startDate":"2026-10-07T19:00:00","endDate":"2026-11-04T23:59:59",
              "beginning":"2026-09-20T00:00:00","deadline":"2026-10-05T23:59:59"
            }]}}
        """.trimIndent()
    }
}
