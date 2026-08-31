package com.ogonggo.userapi.auth.presentation

import com.ogonggo.core.error.BusinessException
import com.ogonggo.userapi.auth.implement.OgonggoTokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 오공고가 발급한 액세스 토큰만 해석한다. 렛츠커리어 토큰은 로그인 교환 경로에서만 사용한다.
 * 토큰이 유효하지 않으면 인증 없이 통과시켜 EntryPoint가 401 계약으로 응답하게 한다.
 */
class UserAuthenticationFilter(
    private val tokenProvider: OgonggoTokenProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val accessToken = request.resolveAccessToken()

        if (accessToken != null) {
            try {
                val userId = tokenProvider.parseAccessToken(accessToken)
                SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    listOf(SimpleGrantedAuthority(USER_AUTHORITY)),
                )
            } catch (exception: BusinessException) {
                log.error("액세스 토큰 인증에 실패했습니다.", exception)
                SecurityContextHolder.clearContext()
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun HttpServletRequest.resolveAccessToken(): String? =
        getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX.length)
            ?.takeIf { it.isNotBlank() }

    companion object {
        const val USER_AUTHORITY = "USER"
        private const val BEARER_PREFIX = "Bearer "
        private val log = LoggerFactory.getLogger(UserAuthenticationFilter::class.java)
    }
}
