package com.ogonggo.adminapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.error.AdminApiErrorCode
import com.ogonggo.adminapi.response.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler

class AdminAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.writeError(ErrorResponse.from(AdminApiErrorCode.UNAUTHORIZED), objectMapper)
    }
}

class AdminAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.writeError(ErrorResponse.from(AdminApiErrorCode.FORBIDDEN), objectMapper)
    }
}

private fun HttpServletResponse.writeError(errorResponse: ErrorResponse, objectMapper: ObjectMapper) {
    characterEncoding = Charsets.UTF_8.name()
    contentType = MediaType.APPLICATION_JSON_VALUE
    status = errorResponse.status
    objectMapper.writeValue(outputStream, errorResponse)
}
