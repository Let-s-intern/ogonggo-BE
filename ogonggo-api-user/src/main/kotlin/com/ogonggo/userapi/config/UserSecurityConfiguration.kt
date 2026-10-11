package com.ogonggo.userapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.userapi.auth.presentation.LetsCareerInternalApiKeyFilter
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import com.ogonggo.userapi.auth.presentation.UserAuthenticationFilter
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.IpAddressMatcher
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
        // 설정 빈 대신 값을 직접 읽는다. 웹 계층 테스트 슬라이스에는 LetsCareerProperties 빈이 없다.
        @Value("\${ogonggo.letscareer.internal-api-key:}") letsCareerInternalApiKey: String,
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
                // Actuator metrics는 loopback 요청만 허용하고 외부 요청은 차단한다. health는 아래 denyAll 안전망을 따른다.
                it.requestMatchers(EndpointRequest.to("metrics")).access { _, context ->
                    AuthorizationDecision(LOOPBACK_MATCHER.matches(context.request))
                }
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
                // 광고 문의는 오공고 계정이 없는 기업 담당자가 소개 페이지에서 남긴다.
                it.requestMatchers(HttpMethod.POST, "/api/v1/advertisement-inquiries").permitAll()
                // 개선 의견은 로그인 없이도 남긴다. 토큰이 있으면 작성자를 함께 기록한다.
                it.requestMatchers(HttpMethod.POST, "/api/v1/service-feedbacks").permitAll()
                // 역할은 토큰에 없으므로 클라이언트는 이 경로로 자기 역할과 프로필을 읽는다.
                it.requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                it.requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/users/me/fcm-token",
                ).authenticated()
                it.requestMatchers(
                    HttpMethod.PUT,
                    "/api/v1/users/me/fcm-token",
                ).authenticated()
                it.requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/users/me/notifications/fcm/test",
                ).authenticated()
                it.requestMatchers(HttpMethod.PUT, "/api/v1/users/me/profile").authenticated()
                it.requestMatchers(
                    HttpMethod.PUT,
                    "/api/v1/users/me/company-profile/basic-info",
                    "/api/v1/users/me/company-profile/manager-info",
                ).authenticated()
                it.requestMatchers(HttpMethod.PUT, "/api/v1/users/me/notification-email").authenticated()
                it.requestMatchers(HttpMethod.PUT, "/api/v1/users/me/profile-image").authenticated()
                it.requestMatchers(HttpMethod.DELETE, "/api/v1/users/me/profile-image").authenticated()
                it.requestMatchers(HttpMethod.PATCH, "/api/v1/users/me/password").authenticated()
                it.requestMatchers("/api/v1/users/me/bootcamps", "/api/v1/users/me/bootcamps/**").authenticated()
                it.requestMatchers("/api/v1/users/me/jobs", "/api/v1/users/me/jobs/**").authenticated()
                it.requestMatchers(HttpMethod.POST, "/api/v1/images").authenticated()
                // 비슷한 공고는 내 희망 직무·산업으로 고르는 사용자별 결과라 채용공고 조회 중 유일하게 로그인을 요구한다.
                // 아래의 조회 전체 허용보다 먼저 선언해야 적용된다.
                it.requestMatchers(HttpMethod.GET, "/api/v1/jobs/similar").authenticated()
                it.requestMatchers(HttpMethod.GET, "/api/v1/recruitment-posts", "/api/v1/recruitment-posts/**").permitAll()
                it.requestMatchers(
                    "/api/v1/users/me/recruitment-post-applications",
                    "/api/v1/users/me/recruitment-post-applications/**",
                    "/api/v1/users/me/recruitment-posts",
                    "/api/v1/users/me/recruitment-posts/**",
                ).authenticated()
                // 프런트가 새 경로로 옮기는 동안 남겨 둔 예전 경로다(RecruitmentPostLegacyPathController). 서버 2차 배포에서 제거한다.
                it.requestMatchers(
                    "/api/v1/me/recruitment-applications",
                    "/api/v1/me/recruitment-applications/**",
                    "/api/v1/me/recruitment-posts",
                    "/api/v1/me/recruitment-posts/**",
                ).authenticated()
                it.requestMatchers(HttpMethod.POST, "/api/v1/recruitment-posts/*/applications").authenticated()
                it.requestMatchers("/api/v1/recruitment-posts", "/api/v1/recruitment-posts/**").authenticated()
                // 채용공고와 부트캠프 조회는 로그인 없이 연다.
                // 액세스 토큰을 보내면 북마크 여부가 채워지고, 없으면 비로그인 응답을 준다.
                it.requestMatchers(HttpMethod.GET, "/api/v1/jobs", "/api/v1/jobs/**").permitAll()
                // 원문 이동 기록은 채용공고 하위의 유일한 쓰기 경로이므로 메서드와 경로를 좁혀 허용한다.
                // 누가 눌렀는지를 남기는 기록이라 조회와 달리 로그인을 요구한다.
                it.requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/source-url-clicks").authenticated()
                it.requestMatchers("/api/v1/job-bookmarks", "/api/v1/job-bookmarks/**").authenticated()
                it.requestMatchers(HttpMethod.GET, "/api/v1/bootcamps", "/api/v1/bootcamps/**").permitAll()
                // enum 선택지는 사용자와 무관한 고정 값이라 로그인 없이 연다.
                it.requestMatchers(HttpMethod.GET, "/api/v1/enums").permitAll()
                // 공지는 관리자 API에서만 작성하고 사용자는 로그인 없이 읽기만 한다.
                it.requestMatchers(HttpMethod.GET, "/api/v1/announcements", "/api/v1/announcements/*").permitAll()
                // 추천 챌린지는 채용공고 조회처럼 로그인 없이 연다. 토큰이 있으면 그 사용자의 렛츠커리어 계정을 추천에 넘긴다.
                it.requestMatchers(HttpMethod.GET, "/api/v1/recommended-challenges").permitAll()
                // 취준고민은 조회만 로그인 없이 연다. 토큰을 보내면 내 글·도움돼요 여부가 채워진다.
                it.requestMatchers(HttpMethod.GET, "/api/v1/concerns", "/api/v1/concerns/**").permitAll()
                it.requestMatchers("/api/v1/concerns", "/api/v1/concerns/**").authenticated()
                // 지원 페이지 이동 기록은 부트캠프 하위의 유일한 쓰기 경로이므로 메서드와 경로를 좁혀 허용한다.
                // 누가 눌렀는지를 남기는 기록이라 조회와 달리 로그인을 요구한다.
                // application-url-clicks는 프런트가 새 경로로 옮기는 동안 남겨 둔 예전 경로다. 서버 2차 배포에서 제거한다.
                it.requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/bootcamps/*/source-url-clicks",
                    "/api/v1/bootcamps/*/application-url-clicks",
                ).authenticated()
                it.requestMatchers("/api/v1/bootcamp-bookmarks", "/api/v1/bootcamp-bookmarks/**").authenticated()
                it.requestMatchers(
                    "/api/v1/recruitment-post-bookmarks",
                    "/api/v1/recruitment-post-bookmarks/**",
                    "/api/v1/recruitment-posts/*/bookmarks/me",
                ).authenticated()
                // 렛츠커리어 서버가 학력·희망 조건 변경을 보내는 서버 간 경로다. 사용자 토큰으로는 부를 수 없다.
                it.requestMatchers(HttpMethod.PUT, "/api/v1/internal/letscareer-users/*/job-profile")
                    .hasAuthority(LetsCareerInternalApiKeyFilter.LETSCAREER_AUTHORITY)
                it.anyRequest().denyAll()
            }
            .addFilterBefore(
                UserAuthenticationFilter(tokenProvider),
                UsernamePasswordAuthenticationFilter::class.java,
            )
            .addFilterAfter(
                LetsCareerInternalApiKeyFilter(letsCareerInternalApiKey),
                UserAuthenticationFilter::class.java,
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
            // 관리자 콘솔 운영 도메인이다. 관리자 API는 토큰을 발급하지 않아 로그인·재발급을 여기서 한다.
            "https://admin.ogonggo.co.kr",
            // 로컬 개발 서버는 프레임워크와 사람마다 포트가 달라 전부 연다.
            "http://localhost:[*]",
            /*
             * API Gateway 주소. 여기서 Swagger UI 를 띄워 API 를 호출한다.
             *
             * 같은 오리진인데도 목록에 필요하다. 브라우저는 GET/HEAD 가 아닌 요청에는 동일 오리진에도
             * Origin 헤더를 붙이고, 스프링은 Origin 이 있으면 CORS 요청으로 처리한다.
             * 앱은 API Gateway 뒤에 있어 자기 공개 주소를 몰라 스스로와 비교할 수도 없다.
             * 목록에 없으면 Swagger 의 POST 가 403 Invalid CORS request 로 막힌다.
             */
            "https://qi9peez04m.execute-api.ap-northeast-2.amazonaws.com",
        )

        private const val PREFLIGHT_MAX_AGE_SECONDS = 3600L
        private val LOOPBACK_MATCHER = IpAddressMatcher("127.0.0.1")
    }
}
