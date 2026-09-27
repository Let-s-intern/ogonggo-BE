package com.ogonggo.core.work24.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.work24.domain.Work24Api
import com.ogonggo.core.work24.error.Work24ErrorCode
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.never
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class Work24ClientTest {

    private val restClientBuilder = RestClient.builder().baseUrl(BASE_URL)
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()

    @Test
    fun `서비스 인증키와 고정 파라미터를 붙여 호출하고 XML 응답을 JSON으로 바꾼다`() {
        // given
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo212L01.do")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("authKey", OCCUPATION_KEY))
            .andExpect(queryParam("returnType", "XML"))
            .andExpect(queryParam("target", "JOBCD"))
            .andExpect(queryParam("keyword", "%EA%B0%9C%EB%B0%9C%7C%EB%94%94%EC%9E%90%EC%9D%B8"))
            .andRespond(
                withSuccess(
                    """
                    <?xml version='1.0' encoding='UTF-8'?>
                    <jobsList>
                      <total>2</total>
                      <jobList><jobCd>1</jobCd><jobNm>개발자</jobNm></jobList>
                      <jobList><jobCd>2</jobCd><jobNm>디자이너</jobNm></jobList>
                    </jobsList>
                    """.trimIndent(),
                    MediaType.APPLICATION_XML,
                ),
            )

        // when
        val result = client().fetch(
            Work24Api.OCCUPATIONS,
            // 호출자가 보낸 인증키와 고정 파라미터는 무시한다.
            mapOf("keyword" to "개발|디자인", "authKey" to "caller-key", "target" to "WRONG", "srchType" to ""),
        )

        // then
        server.verify()
        assertEquals("2", result.at("/total").asText())
        assertEquals(listOf("개발자", "디자이너"), result.at("/jobList").map { it.at("/jobNm").asText() })
    }

    @Test
    fun `같은 이름의 요소가 하나뿐이면 배열이 아니라 객체다`() {
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo212L01.do")))
            .andRespond(
                withSuccess(
                    "<jobsList><jobList><jobNm>개발자</jobNm><empty/></jobList></jobsList>",
                    MediaType.APPLICATION_XML,
                ),
            )

        val result = client().fetch(Work24Api.OCCUPATIONS, emptyMap())

        assertTrue(result.at("/jobList").isObject)
        assertEquals("", result.at("/jobList/empty").asText())
    }

    @Test
    fun `JSON을 지원하는 API는 JSON으로 요청하고 응답을 그대로 돌려준다`() {
        server.expect(requestTo(startsWith("$BASE_URL/wk/callOpenApiSvcInfo215L11.do")))
            .andExpect(queryParam("returnType", "JSON"))
            .andRespond(withSuccess("""{"total": 1, "items": [{"word": "A"}]}""", MediaType.APPLICATION_JSON))

        val result = client().fetch(Work24Api.DUTY_DATA_DICTIONARY, mapOf("word" to "A"))

        assertEquals("A", result.at("/items/0/word").asText())
    }

    @Test
    fun `훈련과정 목록은 XML로 요청하고 목록 출력형태를 고정한다`() {
        server.expect(requestTo(startsWith("$BASE_URL/hr/callOpenApiSvcInfo310L01.do")))
            .andExpect(queryParam("returnType", "XML"))
            .andExpect(queryParam("outType", "1"))
            .andRespond(withSuccess("<HRDNet><scn_cnt>0</scn_cnt><srchList/></HRDNet>", MediaType.APPLICATION_XML))

        val result = client().fetch(Work24Api.TOMORROW_LEARNING_CARD_COURSES, mapOf("outType" to "2"))

        assertEquals("0", result.at("/scn_cnt").asText())
    }

    @Test
    fun `고용24가 본문의 error로 거절하면 요청 거절로 알린다`() {
        server.expect(requestTo(startsWith(BASE_URL)))
            .andRespond(
                withSuccess(
                    "<?xml version='1.0' encoding='UTF-8'?><GO24><error>신청하신 OpenApi 서비스가 존재하지 않습니다</error></GO24>",
                    MediaType.APPLICATION_XML,
                ),
            )

        val exception = assertThrows<InternalServerException> {
            client().fetch(Work24Api.SMALL_GIANT_COMPANIES, emptyMap())
        }

        assertEquals(Work24ErrorCode.WORK24_REQUEST_REJECTED, exception.errorCode)
    }

    @Test
    fun `고용24 호출이 실패하면 호출 실패로 알린다`() {
        server.expect(requestTo(startsWith(BASE_URL))).andRespond(withServerError())

        val exception = assertThrows<InternalServerException> {
            client().fetch(Work24Api.SMALL_GIANT_COMPANIES, emptyMap())
        }

        assertEquals(Work24ErrorCode.WORK24_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `DTD가 있는 응답은 해석하지 않는다`() {
        server.expect(requestTo(startsWith(BASE_URL)))
            .andRespond(
                withSuccess(
                    """<?xml version="1.0"?><!DOCTYPE r [<!ENTITY x SYSTEM "file:///etc/passwd">]><r><v>&x;</v></r>""",
                    MediaType.APPLICATION_XML,
                ),
            )

        val exception = assertThrows<InternalServerException> {
            client().fetch(Work24Api.SMALL_GIANT_COMPANIES, emptyMap())
        }

        assertEquals(Work24ErrorCode.WORK24_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `인증키가 없는 서비스는 호출하지 않고 실패한다`() {
        server.expect(never(), requestTo(startsWith(BASE_URL)))

        val exception = assertThrows<InternalServerException> {
            client().fetch(Work24Api.RECRUITMENTS, mapOf("callTp" to "L"))
        }

        assertEquals(Work24ErrorCode.WORK24_AUTH_KEY_NOT_CONFIGURED, exception.errorCode)
        server.verify()
    }

    @Test
    fun `설정 객체를 문자열로 바꿔도 인증키가 드러나지 않는다`() {
        assertFalse(properties().toString().contains(OCCUPATION_KEY))
    }

    private fun client() = Work24Client(restClientBuilder.build(), properties(), ObjectMapper())

    private fun properties() = Work24Properties(
        baseUrl = BASE_URL,
        occupationAuthKey = OCCUPATION_KEY,
        tomorrowLearningCardAuthKey = "training-key",
        dutyAuthKey = "duty-key",
        smallGiantCompanyAuthKey = "small-giant-key",
    )

    private companion object {
        const val BASE_URL = "https://work24.test/cm/openApi/call"
        const val OCCUPATION_KEY = "occupation-key"
    }
}
