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
import java.time.Duration

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
    fun letsCareerRestClient(properties: LetsCareerProperties): RestClient =
        letsCareerRestClient(properties, properties.readTimeout)

    /**
     * 렛츠커리어 비밀번호 변경 전용 클라이언트다. 응답이 없으면 같은 요청을 다시 보내므로 한 번의 대기를 짧게 둔다.
     * 처음 요청과 재시도를 합한 최악의 시간이 오공고 앞단 API Gateway의 29초 제한 안에 들어와야 한다.
     * 렛츠커리어의 처리는 BCrypt 한 번이라 평소 0.1초 안팎이다.
     *
     * 운영 설정은 시크릿으로 통째 덮어써지므로 누락되지 않도록 설정 키로 빼지 않고 코드에 둔다.
     */
    @Bean
    fun letsCareerPasswordRestClient(properties: LetsCareerProperties): RestClient =
        letsCareerRestClient(properties, PASSWORD_CHANGE_READ_TIMEOUT)

    private fun letsCareerRestClient(properties: LetsCareerProperties, readTimeout: Duration): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(readTimeout)
        }

        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    companion object {
        private val PASSWORD_CHANGE_READ_TIMEOUT: Duration = Duration.ofSeconds(3)
    }
}
