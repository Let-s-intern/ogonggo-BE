package com.ogonggo.core.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.core.work24.domain.Work24Api
import com.ogonggo.core.work24.domain.Work24Service
import com.ogonggo.core.work24.error.Work24ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * 고용24 Open API를 호출하고 응답을 JSON 트리로 돌려준다.
 *
 * API마다 응답 항목이 다르고 아직 어떤 항목을 저장할지 정하지 않았으므로 응답을 타입으로 옮기지 않는다.
 * 저장·가공이 정해지면 그 API만 타입으로 옮긴다.
 */
@Component
class Work24Client internal constructor(
    @Qualifier(WORK24_REST_CLIENT)
    private val work24RestClient: RestClient,
    private val properties: Work24Properties,
    private val objectMapper: ObjectMapper,
) {

    /** 사용 신청이 승인되어 인증키가 설정된 서비스인지 알려 준다. */
    fun isConfigured(service: Work24Service): Boolean = properties.authKey(service).isNotBlank()

    /**
     * [parameters]는 고용24 명세의 요청 파라미터를 그대로 넘긴다.
     * 인증키, 응답 형식, 명세가 고정한 값은 호출자가 보내도 무시하고 [Work24Api]에 정한 값으로 보낸다.
     * 값이 빈 파라미터는 보내지 않는다. 고용24는 "전체" 조건을 파라미터를 빼는 것으로 표현한다.
     */
    fun fetch(api: Work24Api, parameters: Map<String, String>): JsonNode {
        val authKey = properties.authKey(api.service)
        if (authKey.isBlank()) {
            log.error("고용24 {} 인증키가 설정되지 않았습니다. api={}", api.service.desc, api)
            throw InternalServerException(Work24ErrorCode.WORK24_AUTH_KEY_NOT_CONFIGURED)
        }

        val query = requestParameters(api, parameters) + (Work24Api.AUTH_KEY to authKey)
        val body = try {
            work24RestClient.get()
                .uri { builder ->
                    // 값을 URI 템플릿 변수로 넘겨야 `{`, `+`, `|`가 들어간 값도 그대로 인코딩된다.
                    builder.path(api.path)
                    query.keys.forEachIndexed { index, name -> builder.queryParam(name, "{p$index}") }
                    builder.build(query.values.withIndex().associate { (index, value) -> "p$index" to value })
                }
                .retrieve()
                // 명세가 UTF-8로 정해져 있다. 응답 헤더에 charset이 빠져도 한글이 깨지지 않도록 직접 디코딩한다.
                .body(ByteArray::class.java)
                ?.toString(Charsets.UTF_8)
        } catch (exception: RestClientException) {
            // 예외 메시지에 인증키가 들어간 요청 URL이 실릴 수 있어 원본 예외를 그대로 남기지 않는다.
            log.error(
                "고용24 호출에 실패했습니다. api={}, exception={}, message={}",
                api,
                exception.javaClass.name,
                exception.message?.replace(authKey, MASKED),
            )
            throw InternalServerException(Work24ErrorCode.WORK24_UNAVAILABLE)
        }

        val result = parse(api, body)
        result.get(ERROR_FIELD)?.let { error ->
            log.error("고용24가 요청을 거절했습니다. api={}, error={}", api, error.asText())
            throw InternalServerException(Work24ErrorCode.WORK24_REQUEST_REJECTED)
        }
        return result
    }

    private fun parse(api: Work24Api, body: String?): JsonNode {
        val text = body?.trim().orEmpty()
        if (text.isEmpty()) {
            log.error("고용24 응답 본문이 비어 있습니다. api={}", api)
            throw InternalServerException(Work24ErrorCode.WORK24_UNAVAILABLE)
        }

        return try {
            // 요청한 형식이 아니라 본문 모양으로 판단해, 명세와 다른 형식이 와도 해석한다.
            if (text.startsWith("<")) Work24XmlConverter.toJson(text) else objectMapper.readTree(text)
        } catch (exception: Exception) {
            log.error("고용24 응답을 해석하지 못했습니다. api={}, body={}", api, text.take(MAX_LOGGED_BODY), exception)
            throw InternalServerException(Work24ErrorCode.WORK24_UNAVAILABLE)
        }
    }

    companion object {
        private const val ERROR_FIELD = "error"
        private const val MASKED = "****"
        private const val MAX_LOGGED_BODY = 500
        private val log = LoggerFactory.getLogger(Work24Client::class.java)

        internal fun requestParameters(api: Work24Api, parameters: Map<String, String>): Map<String, String> {
            val reserved = api.reservedParameters()
            val passed = parameters
                .filterKeys { it != Work24Api.AUTH_KEY && it !in reserved }
                .filterValues { it.isNotBlank() }
            return passed + reserved
        }
    }
}
