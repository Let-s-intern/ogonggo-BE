package com.ogonggo.userapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.auth.presentation.UserAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
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
class UserSecurityConfiguration {

    @Bean
    fun userAuthenticationEntryPoint(objectMapper: ObjectMapper): UserAuthenticationEntryPoint =
        UserAuthenticationEntryPoint(objectMapper)

    @Bean
    fun userAccessDeniedHandler(objectMapper: ObjectMapper): UserAccessDeniedHandler =
        UserAccessDeniedHandler(objectMapper)

    @Bean
    fun userSecurityFilterChain(
        http: HttpSecurity,
        tokenProvider: OgonggoTokenProvider,
        userAuthenticationEntryPoint: UserAuthenticationEntryPoint,
        userAccessDeniedHandler: UserAccessDeniedHandler,
    ): SecurityFilterChain =
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(userAuthenticationEntryPoint)
                it.accessDeniedHandler(userAccessDeniedHandler)
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
                // 렛츠커리어 토큰 교환과 재발급은 오공고 세션이 없는 상태에서 호출한다.
                it.requestMatchers(HttpMethod.POST, "/api/v1/auth/letscareer", "/api/v1/auth/token").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/auth/signout").authenticated()
                // 기업 회원가입과 로그인은 오공고 세션이 없는 상태에서 호출한다.
                it.requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/company/signup",
                    "/api/v1/auth/company/signin",
                ).permitAll()
                it.requestMatchers("/api/v1/users/me/bootcamps", "/api/v1/users/me/bootcamps/**").authenticated()
                // 채용공고와 부트캠프 조회는 로그인 없이 연다.
                // 액세스 토큰을 보내면 북마크 여부가 채워지고, 없으면 비로그인 응답을 준다.
                it.requestMatchers(HttpMethod.GET, "/api/v1/jobs", "/api/v1/jobs/**").permitAll()
                // 원문 이동 기록은 채용공고 하위의 유일한 쓰기 경로이므로 메서드와 경로를 좁혀 허용한다.
                // 누가 눌렀는지를 남기는 기록이라 조회와 달리 로그인을 요구한다.
                it.requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/source-url-clicks").authenticated()
                it.requestMatchers("/api/v1/job-bookmarks", "/api/v1/job-bookmarks/**").authenticated()
                it.requestMatchers(HttpMethod.GET, "/api/v1/bootcamps", "/api/v1/bootcamps/**").permitAll()
                it.anyRequest().denyAll()
            }
            .addFilterBefore(
                UserAuthenticationFilter(tokenProvider),
                UsernamePasswordAuthenticationFilter::class.java,
            )
            .build()

    /**
     * 브라우저에서 사용자 API를 호출하는 오리진만 연다.
     * 오리진을 명시하므로 allowCredentials를 켜도 임의 사이트가 자격증명을 실어 보낼 수 없다.
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
         * 오리진을 추가하려면 이 목록을 고치고 배포한다.
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
