package com.ogonggo.adminapi.auth.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.implement.AdminAccessTokenParser
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 관리자 콘솔 경로의 액세스 토큰을 해석하고 관리자인지 확인한다.
 *
 * 토큰이 유효하면 관리자가 아니어도 인증은 남긴다. 그래야 토큰 없는 요청은 401, 관리자가 아닌 요청은 403으로 나뉜다.
 * 토큰이 없거나 유효하지 않으면 인증 없이 통과시켜 EntryPoint가 401로 응답하게 한다.
 * 크롤러의 내부 경로는 사용자 조회가 필요 없으므로 이 필터를 거치지 않는다.
 */
class AdminAuthenticationFilter(
    private val tokenParser: AdminAccessTokenParser,
    private val adminAuthService: AdminAuthService,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith(ADMIN_API_PATH_PREFIX)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userId = request.resolveAccessToken()?.let(tokenParser::parseUserId)

        if (userId != null) {
            val authorities = if (adminAuthService.isActiveAdmin(userId)) {
                listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY))
            } else {
                emptyList()
            }
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(userId, null, authorities)
        }

        filterChain.doFilter(request, response)
    }

    private fun HttpServletRequest.resolveAccessToken(): String? =
        getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX.length)
            ?.takeIf { it.isNotBlank() }

    companion object {
        const val ADMIN_AUTHORITY = "ROLE_ADMIN"
        const val ADMIN_API_PATH_PREFIX = "/api/v1/admin/"
        private const val BEARER_PREFIX = "Bearer "
    }
}
