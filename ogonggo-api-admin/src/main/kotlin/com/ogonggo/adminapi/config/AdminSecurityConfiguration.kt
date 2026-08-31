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
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(adminAuthenticationEntryPoint)
                it.accessDeniedHandler(adminAccessDeniedHandler)
            }
            .authorizeHttpRequests {
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
}
