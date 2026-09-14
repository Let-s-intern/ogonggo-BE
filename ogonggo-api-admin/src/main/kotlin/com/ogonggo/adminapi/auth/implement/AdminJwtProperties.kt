package com.ogonggo.adminapi.auth.implement

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 관리자도 사용자 API의 로그인으로 액세스 토큰을 받으므로 사용자 API와 같은 서명 시크릿을 쓴다.
 * 값을 설정하지 않으면 어떤 토큰도 검증하지 못해 관리자 API를 전면 차단한다.
 */
@ConfigurationProperties(prefix = "ogonggo.auth.jwt")
data class AdminJwtProperties(
    /** 사용자 API의 `ogonggo.auth.jwt.secret`과 같은 Base64 HS512 시크릿. */
    val secret: String = "",
)
