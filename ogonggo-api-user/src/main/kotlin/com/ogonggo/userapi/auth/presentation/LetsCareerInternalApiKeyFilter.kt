package com.ogonggo.userapi.auth.presentation

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

/**
 * 렛츠커리어 서버가 부르는 내부 경로를 API 키 헤더로 인증한다.
 * 키는 오공고가 렛츠커리어를 부를 때 쓰는 `ogonggo.letscareer.internal-api-key`와 같다. 두 서비스가 공유하는 비밀이다.
 * 키가 없거나 다르면 인증을 남기지 않고 통과시키며, 접근 거부는 Security 설정이 판단한다.
 */
class LetsCareerInternalApiKeyFilter(
    private val internalApiKey: String,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith(INTERNAL_PATH_PREFIX)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val presentedKey = request.getHeader(INTERNAL_API_KEY_HEADER)

        if (presentedKey != null && matchesConfiguredKey(presentedKey)) {
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                LETSCAREER_PRINCIPAL,
                null,
                listOf(SimpleGrantedAuthority(LETSCAREER_AUTHORITY)),
            )
        }

        filterChain.doFilter(request, response)
    }

    /** 비교 시간으로 키를 추측할 수 없도록 상수 시간 비교를 사용한다. 키가 설정되지 않으면 모두 거부한다. */
    private fun matchesConfiguredKey(presentedKey: String): Boolean {
        if (internalApiKey.isBlank()) {
            return false
        }

        return MessageDigest.isEqual(
            presentedKey.toByteArray(Charsets.UTF_8),
            internalApiKey.toByteArray(Charsets.UTF_8),
        )
    }

    companion object {
        const val INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key"
        const val LETSCAREER_AUTHORITY = "ROLE_LETSCAREER"
        private const val INTERNAL_PATH_PREFIX = "/api/v1/internal/"
        private const val LETSCAREER_PRINCIPAL = "letscareer"
    }
}
