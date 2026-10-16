package com.ogonggo.userapi.auth.implement

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "ogonggo.auth.jwt")
data class JwtProperties(
    /** HS512 서명에 사용하는 Base64 인코딩 시크릿. 렛츠커리어 시크릿과 공유하지 않는다. */
    val secret: String,
    val accessTokenValidity: Duration = Duration.ofMinutes(30),
    val refreshTokenValidity: Duration = Duration.ofDays(14),
)

@ConfigurationProperties(prefix = "ogonggo.letscareer")
data class LetsCareerProperties(
    val baseUrl: String,
    val internalApiKey: String,
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(5),
    /**
     * 렛츠커리어 SSO 화이트리스트에 등록된 오공고의 서비스 이름(sso_redirect_whitelist.service_name).
     * verify 응답의 firstSignInService가 이 값이면 오공고로 처음 들어온 회원이다.
     * 비워 두면 렛츠커리어에서 처음 로그인한 회원만 첫 로그인 서비스로 가르고, 나머지는 가입 일시로 가른다.
     */
    val ssoServiceName: String? = null,
)
