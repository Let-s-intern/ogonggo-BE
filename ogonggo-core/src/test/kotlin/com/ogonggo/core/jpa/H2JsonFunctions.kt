package com.ogonggo.core.jpa

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * 운영 DB(MySQL)의 JSON 함수를 테스트용 H2에 흉내 낸다. H2에는 `json_extract`가 없다.
 * 테스트 클래스에 [CREATE_ALIAS]를 `@Sql`로 걸어 쓴다.
 */
object H2JsonFunctions {

    const val CREATE_ALIAS =
        "CREATE ALIAS IF NOT EXISTS JSON_EXTRACT FOR \"com.ogonggo.core.jpa.H2JsonFunctions.jsonExtract\""

    private val objectMapper = ObjectMapper()

    /** `$.이름` 꼴의 최상위 경로만 지원한다. 값이 없으면 MySQL처럼 null이다. */
    @JvmStatic
    fun jsonExtract(json: String?, path: String?): String? {
        if (json == null || path == null) return null
        require(path.startsWith("$.")) { "지원하지 않는 JSON 경로입니다: $path" }
        val node = objectMapper.readTree(json).get(path.removePrefix("$.")) ?: return null
        return objectMapper.writeValueAsString(node)
    }
}
