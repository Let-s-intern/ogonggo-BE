package com.ogonggo.adminapi.internal.implement

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

/**
 * 내부 클라이언트를 API 키 헤더만으로 인증한다.
 * 키가 없거나 다르면 인증을 남기지 않고 통과시키며, 접근 거부는 Security 설정이 판단한다.
 */
class InternalApiKeyAuthenticationFilter(
    private val properties: InternalApiProperties,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val presentedKey = request.getHeader(INTERNAL_API_KEY_HEADER)

        if (presentedKey != null && matchesConfiguredKey(presentedKey)) {
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                INTERNAL_CLIENT_PRINCIPAL,
                null,
                listOf(SimpleGrantedAuthority(INTERNAL_CLIENT_AUTHORITY)),
            )
        }

        filterChain.doFilter(request, response)
    }

    /** 비교 시간으로 키를 추측할 수 없도록 상수 시간 비교를 사용한다. */
    private fun matchesConfiguredKey(presentedKey: String): Boolean {
        if (properties.apiKey.isBlank()) {
            return false
        }

        return MessageDigest.isEqual(
            presentedKey.toByteArray(Charsets.UTF_8),
            properties.apiKey.toByteArray(Charsets.UTF_8),
        )
    }

    companion object {
        const val INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key"
        const val INTERNAL_CLIENT_AUTHORITY = "ROLE_INTERNAL"
        private const val INTERNAL_CLIENT_PRINCIPAL = "internal-client"
    }
}
