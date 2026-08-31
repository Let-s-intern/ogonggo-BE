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
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(userAuthenticationEntryPoint)
                it.accessDeniedHandler(userAccessDeniedHandler)
            }
            .authorizeHttpRequests {
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
                it.requestMatchers(HttpMethod.GET, "/api/v1/jobs", "/api/v1/jobs/**").authenticated()
                // 원문 이동 기록은 채용공고 하위의 유일한 쓰기 경로이므로 메서드와 경로를 좁혀 허용한다.
                it.requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/source-url-clicks").authenticated()
                it.requestMatchers("/api/v1/job-bookmarks", "/api/v1/job-bookmarks/**").authenticated()
                it.requestMatchers(HttpMethod.GET, "/api/v1/bootcamps", "/api/v1/bootcamps/**").authenticated()
                it.anyRequest().denyAll()
            }
            .addFilterBefore(
                UserAuthenticationFilter(tokenProvider),
                UsernamePasswordAuthenticationFilter::class.java,
            )
            .build()
}
