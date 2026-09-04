package com.ogonggo.adminapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.internal.implement.InternalApiKeyAuthenticationFilter
import com.ogonggo.adminapi.internal.implement.InternalApiProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.CorsUtils
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(InternalApiProperties::class)
class AdminSecurityConfiguration {

    @Bean
    fun adminAuthenticationEntryPoint(objectMapper: ObjectMapper): AdminAuthenticationEntryPoint =
        AdminAuthenticationEntryPoint(objectMapper)

    @Bean
    fun adminAccessDeniedHandler(objectMapper: ObjectMapper): AdminAccessDeniedHandler =
        AdminAccessDeniedHandler(objectMapper)

    @Bean
    fun adminSecurityFilterChain(
        http: HttpSecurity,
        adminAuthenticationEntryPoint: AdminAuthenticationEntryPoint,
        adminAccessDeniedHandler: AdminAccessDeniedHandler,
        internalApiProperties: InternalApiProperties,
    ): SecurityFilterChain =
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(adminAuthenticationEntryPoint)
                it.accessDeniedHandler(adminAccessDeniedHandler)
            }
            .authorizeHttpRequests {
                // CorsFilter가 인가보다 앞에 있어 정상 preflight는 여기까지 오지 않는다.
                // anyRequest().denyAll()로 끝나는 체인이라 안전망으로 함께 둔다.
                it.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                it.requestMatchers(
                    "/health",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                ).permitAll()
                it.requestMatchers("/api/v1/internal/**")
                    .hasAuthority(InternalApiKeyAuthenticationFilter.INTERNAL_CLIENT_AUTHORITY)
                it.anyRequest().denyAll()
            }
            .addFilterBefore(
                InternalApiKeyAuthenticationFilter(internalApiProperties),
                UsernamePasswordAuthenticationFilter::class.java,
            )
            .build()

    /**
     * 브라우저에서 관리자 API를 호출하는 오리진만 연다.
     * 내부 API 경로는 크롤러의 서버 간 호출이라 CORS와 무관하지만,
     * CORS는 브라우저만 강제하는 규칙이므로 전체 경로 하나로 등록해도 서버 간 호출에 영향이 없다.
     */
    private fun corsConfigurationSource(): CorsConfigurationSource =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                CorsConfiguration().apply {
                    allowedOriginPatterns = ALLOWED_ORIGIN_PATTERNS
                    allowedMethods = listOf("*")
                    allowedHeaders = listOf("*")
                    allowCredentials = true
                    maxAge = PREFLIGHT_MAX_AGE_SECONDS
                },
            )
        }

    companion object {
        /**
         * application.yml은 배포 시 GitHub Secret으로 통째 덮어써지므로 오리진은 코드로 관리한다.
         * 관리자 화면 도메인이 정해지면 사용자 API와 무관하게 이 목록만 바뀐다.
         *
         * `allowedOrigins`가 아니라 `allowedOriginPatterns`를 쓰는 이유는 두 가지다.
         * 전자는 와일드카드를 받지 않아 포트를 열 수 없고, `*` 하나만 넣으면
         * `allowCredentials = true`와 함께 쓸 수 없다. 후자는 요청 오리진을 그대로 되돌려주므로
         * 자격증명을 켠 채로 패턴을 쓸 수 있다.
         */
        private val ALLOWED_ORIGIN_PATTERNS = listOf(
            "https://www.ogonggo.co.kr",
            "https://ogonggo.co.kr",
            // 로컬 개발 서버는 프레임워크와 사람마다 포트가 달라 전부 연다.
            "http://localhost:[*]",
        )

        private const val PREFLIGHT_MAX_AGE_SECONDS = 3600L
    }
}
