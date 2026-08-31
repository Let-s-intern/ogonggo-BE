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
)
