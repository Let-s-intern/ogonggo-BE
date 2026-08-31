package com.ogonggo.userapi.config

import com.ogonggo.userapi.auth.implement.JwtProperties
import com.ogonggo.userapi.auth.implement.LetsCareerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties::class, LetsCareerProperties::class)
class UserAuthConfiguration {

    /** 기업 회원 비밀번호 인코딩. 오공고가 자격증명을 소유하는 유일한 지점이다. */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /**
     * 렛츠커리어 내부 API 전용 클라이언트다.
     * 로그인 교환 경로에서만 사용하므로 타임아웃을 짧게 두어 장애가 길게 전파되지 않도록 한다.
     */
    @Bean
    fun letsCareerRestClient(properties: LetsCareerProperties): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }

        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
