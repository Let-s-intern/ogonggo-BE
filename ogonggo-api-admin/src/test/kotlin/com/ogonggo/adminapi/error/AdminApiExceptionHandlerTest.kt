package com.ogonggo.adminapi.error

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.adminapi.config.AdminAccessDeniedHandler
import com.ogonggo.adminapi.config.AdminAuthenticationEntryPoint
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.BindException
import org.springframework.web.servlet.resource.NoResourceFoundException

class AdminApiExceptionHandlerTest {

    private val handler = AdminApiExceptionHandler()

    @Test
    fun `비즈니스 예외를 ErrorCode의 응답으로 변환한다`() {
        val response = handler.handleBusiness(ConflictException(TestErrorCode.CONFLICT))

        assertEquals(409, response.statusCode.value())
        assertEquals(ErrorResponse(409, "CONFLICT", "이미 존재하는 리소스입니다."), response.body)
    }

    @Test
    fun `검증 오류를 필드명 순서로 결합한다`() {
        val exception = BindException(ValidationTarget(), "request")
        exception.bindingResult.rejectValue("title", "NotBlank", "비어 있을 수 없습니다.")
        exception.bindingResult.rejectValue("companyName", "NotBlank", "비어 있을 수 없습니다.")

        val response = handler.handleBind(exception)

        assertEquals("BAD_REQUEST", response.body?.code)
        assertEquals(
            "[companyName] 비어 있을 수 없습니다., [title] 비어 있을 수 없습니다.",
            response.body?.message,
        )
    }

    @Test
    fun `낙관적 락과 예상하지 못한 예외를 구분한다`() {
        val optimistic = handler.handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException(Any::class.java, 1L),
        )
        val unexpected = handler.handleException(IllegalArgumentException("내부 구현 메시지"))

        assertEquals("OPTIMISTIC_LOCK_CONFLICT", optimistic.body?.code)
        assertEquals(500, unexpected.statusCode.value())
        assertEquals(AdminApiErrorCode.INTERNAL_SERVER_ERROR.message, unexpected.body?.message)
        assertFalse(unexpected.body?.message.orEmpty().contains("내부 구현 메시지"))
    }

    @Test
    fun `매핑되지 않은 경로를 도메인 조회 실패와 구분한다`() {
        val response = handler.handleNoResourceFound(NoResourceFoundException(HttpMethod.GET, "/missing"))

        assertEquals(404, response.statusCode.value())
        assertEquals("API_NOT_FOUND", response.body?.code)
        assertEquals("요청한 API를 찾을 수 없습니다.", response.body?.message)
    }

    @Test
    fun `Security 오류도 동일한 JSON 계약을 사용한다`() {
        val objectMapper = ObjectMapper()
        val request = MockHttpServletRequest()
        val unauthorizedResponse = MockHttpServletResponse()
        val forbiddenResponse = MockHttpServletResponse()

        AdminAuthenticationEntryPoint(objectMapper).commence(
            request,
            unauthorizedResponse,
            Mockito.mock(AuthenticationException::class.java),
        )
        AdminAccessDeniedHandler(objectMapper).handle(
            request,
            forbiddenResponse,
            AccessDeniedException("forbidden"),
        )

        assertEquals(401, unauthorizedResponse.status)
        assertEquals("UNAUTHORIZED", objectMapper.readTree(unauthorizedResponse.contentAsString)["code"].asText())
        assertEquals(403, forbiddenResponse.status)
        assertEquals("FORBIDDEN", objectMapper.readTree(forbiddenResponse.contentAsString)["code"].asText())
    }

    private data class ValidationTarget(
        var companyName: String? = null,
        var title: String? = null,
    )

    private enum class TestErrorCode(
        override val httpStatus: HttpStatus,
        override val message: String,
    ) : ErrorCode {
        CONFLICT(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),
    }
}
