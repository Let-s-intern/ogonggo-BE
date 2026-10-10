package com.ogonggo.userapi.notification.channel.alimtalk

import com.ogonggo.core.notification.domain.NotificationFailureCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import org.springframework.web.client.ResourceAccessException

class NhnAlimTalkClientTest {

    @Test
    fun `가입 템플릿 변수만 NHN 알림톡 요청으로 전달한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = NhnAlimTalkClient(
            restClient = builder.build(),
            properties = NhnAlimTalkProperties(
                appKey = APP_KEY,
                secretKey = SECRET_KEY,
                sendKey = SEND_KEY,
            ),
        )
        val request = AlimTalkMessage(
            recipientNo = PHONE_NUMBER,
            templateCode = "sign_up_confirm",
            templateParameters = mapOf(
                "name" to "홍길동",
                "userEmail" to "hong@example.com",
                "loginType" to "카카오톡 로그인",
                "createDate" to "2026-10-04",
            ),
            idempotencyKey = IDEMPOTENCY_KEY,
        )
        val response = """
            {
              "header": {"resultCode": 0, "resultMessage": "success", "isSuccessful": true},
              "message": {
                "requestId": "request-123",
                "sendResults": [{"recipientSeq": 1, "recipientNo": "$PHONE_NUMBER", "resultCode": 0}]
              }
            }
        """.trimIndent()

        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header("X-Secret-Key", SECRET_KEY))
            .andExpect(header("X-NC-API-IDEMPOTENCY-KEY", IDEMPOTENCY_KEY))
            .andExpect(jsonPath("$.senderKey").value(SEND_KEY))
            .andExpect(jsonPath("$.templateCode").value("sign_up_confirm"))
            .andExpect(jsonPath("$.recipientList.length()").value(1))
            .andExpect(jsonPath("$.recipientList[0].recipientNo").value(PHONE_NUMBER))
            .andExpect(jsonPath("$.recipientList[0].templateParameter.name").value("홍길동"))
            .andExpect(jsonPath("$.recipientList[0].templateParameter.userEmail").value("hong@example.com"))
            .andExpect(jsonPath("$.recipientList[0].templateParameter.loginType").value("카카오톡 로그인"))
            .andExpect(jsonPath("$.recipientList[0].templateParameter.createDate").value("2026-10-04"))
            .andExpect(jsonPath("$.message").doesNotExist())
            .andExpect(jsonPath("$.button").doesNotExist())
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON))

        val result = client.send(request)

        assertInstanceOf(NhnAlimTalkResult.Accepted::class.java, result)
        assertEquals("request-123", (result as NhnAlimTalkResult.Accepted).requestId)
        server.verify()
    }

    @Test
    fun `스크랩 리마인드의 하이픈 포함 변수명을 그대로 전달한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = NhnAlimTalkClient(
            restClient = builder.build(),
            properties = NhnAlimTalkProperties(
                appKey = APP_KEY,
                secretKey = SECRET_KEY,
                sendKey = SEND_KEY,
            ),
        )
        val request = AlimTalkMessage(
            recipientNo = PHONE_NUMBER,
            templateCode = "clip_remind",
            templateParameters = mapOf(
                "name" to "홍길동",
                "posting-title" to "서버 개발자 채용",
            ),
        )

        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andExpect(content().json(
                """
                    {
                      "senderKey": "$SEND_KEY",
                      "templateCode": "clip_remind",
                      "recipientList": [{
                        "recipientNo": "$PHONE_NUMBER",
                        "templateParameter": {
                          "name": "홍길동",
                          "posting-title": "서버 개발자 채용"
                        }
                      }]
                    }
                """.trimIndent(),
                true,
            ))
            .andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON))

        val result = client.send(request)

        assertInstanceOf(NhnAlimTalkResult.Accepted::class.java, result)
        server.verify()
    }

    @Test
    fun `NHN이 본문에서 요청 실패를 응답하면 접수 성공으로 처리하지 않는다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andRespond(withSuccess(
                """
                    {
                      "header": {"resultCode": 400101, "resultMessage": "rejected", "isSuccessful": false}
                    }
                """.trimIndent(),
                MediaType.APPLICATION_JSON,
            ))

        val result = client.send(message())

        assertEquals(
            NhnAlimTalkResult.Error(
                resultCode = "400101",
                failureCategory = NotificationFailureCategory.UNKNOWN_PROVIDER_ERROR,
                description = "매핑되지 않은 NHN API 오류 코드",
            ),
            result,
        )
        server.verify()
    }

    @Test
    fun `NHN 멱등성 중복 코드는 의미 카테고리로 분류한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andRespond(withSuccess(
                """
                    {
                      "header": {
                        "resultCode": -1005,
                        "resultMessage": "The same 'X-NC-API-IDEMPOTENCY-KEY' was used within the last 10 minute.",
                        "isSuccessful": false
                      }
                    }
                """.trimIndent(),
                MediaType.APPLICATION_JSON,
            ))

        val result = client.send(message())

        assertEquals(
            NhnAlimTalkResult.Error(
                resultCode = "-1005",
                failureCategory = NotificationFailureCategory.DUPLICATE_REQUEST,
                description = "동일한 멱등성 키로 10분 이내 요청됨. 이전 요청의 최종 전달 여부는 별도 확인 필요",
            ),
            result,
        )
        server.verify()
    }

    @Test
    fun `미승인 템플릿 오류는 의미 카테고리와 설명으로 분류한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andRespond(withSuccess(
                """
                    {"header":{"resultCode":-3005,"resultMessage":"not approved","isSuccessful":false}}
                """.trimIndent(),
                MediaType.APPLICATION_JSON,
            ))

        val result = client.send(message())

        assertEquals(
            NhnAlimTalkResult.Error(
                resultCode = "-3005",
                failureCategory = NotificationFailureCategory.TEMPLATE_CONFIGURATION,
                description = "템플릿 승인 상태 오류",
            ),
            result,
        )
        server.verify()
    }

    @Test
    fun `요청 필드 검증 오류는 잘못된 요청으로 분류한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andRespond(withSuccess(
                """{"header":{"resultCode":-2001,"isSuccessful":false}}""",
                MediaType.APPLICATION_JSON,
            ))

        val result = client.send(message())

        assertEquals(
            NhnAlimTalkResult.Error(
                resultCode = "-2001",
                failureCategory = NotificationFailureCategory.INVALID_REQUEST,
                description = "요청 파라미터 검증 오류",
            ),
            result,
        )
        server.verify()
    }

    @Test
    fun `수신자별 발송 실패를 전체 요청 접수 성공으로 취급하지 않는다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andRespond(withSuccess(
                """
                    {
                      "header": {"resultCode": 0, "isSuccessful": true},
                      "message": {
                        "requestId": "request-123",
                        "sendResults": [{"recipientSeq": 1, "recipientNo": "$PHONE_NUMBER", "resultCode": 410201}]
                      }
                    }
                """.trimIndent(),
                MediaType.APPLICATION_JSON,
            ))

        val result = client.send(message())

        assertEquals(
            NhnAlimTalkResult.Error(
                resultCode = "410201",
                failureCategory = NotificationFailureCategory.UNKNOWN_PROVIDER_ERROR,
                description = "매핑되지 않은 NHN API 오류 코드",
            ),
            result,
        )
        server.verify()
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "",
        "{}",
        """{"header": {"isSuccessful": true}}""",
        """{"header": {"isSuccessful": true}, "message": {"sendResults": []}}""",
        """{"header": {"isSuccessful": true}, "message": {"sendResults": [{}]}}""",
        """{"header": {"isSuccessful": true}, "message": {"sendResults": [{"resultCode": 0}, {"resultCode": 0}]}}""",
        """{"message": {"sendResults": [{"resultCode": 410201}]}}""",
    ])
    fun `접수 여부를 판단할 수 없는 응답은 명시적 거절과 구분한다`(responseBody: String) {
        // given
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON))

        // when
        val result = client.send(message())

        // then
        assertEquals(NhnAlimTalkResult.InvalidResponse, result)
        server.verify()
    }

    @Test
    fun `HTTP 오류 응답은 통신 예외가 아닌 명시 실패로 반환하고 민감값을 남기지 않는다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        val responseBody = "secret=$SECRET_KEY phone=$PHONE_NUMBER email=hong@example.com full-response"
        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andRespond(
                withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody),
            )
        val logger = LoggerFactory.getLogger(NhnAlimTalkClient::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply {
            context = logger.loggerContext
            start()
        }
        logger.addAppender(appender)

        val result = try {
            client.send(message())
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }

        val output = appender.list.map { it.formattedMessage }.joinToString(" ")
        assertEquals(
            NhnAlimTalkResult.Error(
                resultCode = "NHN_HTTP_500",
                failureCategory = NotificationFailureCategory.PROVIDER_UNAVAILABLE,
                description = "NHN HTTP 응답 오류 (500)",
            ),
            result,
        )
        assertTrue(appender.list.any { it.level == Level.WARN })
        listOf(APP_KEY, SECRET_KEY, PHONE_NUMBER, "hong@example.com", responseBody).forEach { sensitiveValue ->
            assertFalse(output.contains(sensitiveValue), "민감값이 로그 또는 예외에 노출됐습니다: $sensitiveValue")
        }
        server.verify()
    }

    @Test
    fun `HTTP 429는 rate limit 분류로 남긴다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("rate limited"))

        val result = client.send(message())

        assertEquals(
            NhnAlimTalkResult.Error(
                resultCode = "NHN_HTTP_429",
                failureCategory = NotificationFailureCategory.RATE_LIMITED,
                description = "NHN HTTP 응답 오류 (429)",
            ),
            result,
        )
        server.verify()
    }

    @Test
    fun `응답을 받지 못한 통신 오류도 재시도 없이 최종 실패로 반환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andRespond { throw ResourceAccessException("connection reset") }

        assertEquals(
            NhnAlimTalkResult.Error(
                resultCode = "NHN_TRANSPORT_ERROR",
                failureCategory = NotificationFailureCategory.TRANSPORT_ERROR,
                description = "NHN 응답을 받기 전 통신 오류",
            ),
            client.send(message()),
        )
        server.verify()
    }

    @Test
    fun `계약을 해석할 수 없는 응답 오류는 재시도 불가 코드로 정규화한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = client(builder)
        server.expect(requestTo("$NHN_BASE_URL/alimtalk/v2.3/appkeys/$APP_KEY/messages"))
            .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON))

        val result = client.send(message())

        assertEquals(
            NhnAlimTalkResult.Error(
                resultCode = "NHN_CLIENT_ERROR",
                failureCategory = NotificationFailureCategory.CLIENT_ERROR,
                description = "NHN 요청 클라이언트 처리 오류",
            ),
            result,
        )
        server.verify()
    }

    companion object {
        private const val APP_KEY = "test-app-key"
        private const val SECRET_KEY = "test-secret-key"
        private const val SEND_KEY = "test-send-key"
        private const val PHONE_NUMBER = "01012345678"
        private const val IDEMPOTENCY_KEY = "ogonggo-signup-test-key"
        private const val NHN_BASE_URL = "https://api-alimtalk.cloud.toast.com"
        private const val SUCCESS_RESPONSE = """
            {
              "header": {"resultCode": 0, "resultMessage": "success", "isSuccessful": true},
              "message": {
                "requestId": "request-123",
                "sendResults": [{"recipientSeq": 1, "recipientNo": "01012345678", "resultCode": 0}]
              }
            }
        """
    }

    private fun client(builder: RestClient.Builder): NhnAlimTalkClient = NhnAlimTalkClient(
        restClient = builder.build(),
        properties = NhnAlimTalkProperties(
            appKey = APP_KEY,
            secretKey = SECRET_KEY,
            sendKey = SEND_KEY,
        ),
    )

    private fun message(): AlimTalkMessage = AlimTalkMessage(
        recipientNo = PHONE_NUMBER,
        templateCode = "sign_up_confirm",
        templateParameters = mapOf("name" to "홍길동"),
    )
}
