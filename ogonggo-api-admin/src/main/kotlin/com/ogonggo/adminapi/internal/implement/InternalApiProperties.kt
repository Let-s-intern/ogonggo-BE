package com.ogonggo.adminapi.internal.implement

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 크롤러처럼 사람이 아닌 내부 클라이언트가 사용하는 API 키다.
 * 값을 설정하지 않으면 내부 API를 전면 차단한다.
 */
@ConfigurationProperties(prefix = "ogonggo.admin.internal")
data class InternalApiProperties(
    val apiKey: String = "",
)
