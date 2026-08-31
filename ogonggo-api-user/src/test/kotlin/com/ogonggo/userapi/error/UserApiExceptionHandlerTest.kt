package com.ogonggo.userapi.error

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.userapi.config.UserAccessDeniedHandler
import com.ogonggo.userapi.config.UserAuthenticationEntryPoint
import com.ogonggo.userapi.response.ErrorResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.BindException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.servlet.resource.NoResourceFoundException

class UserApiExceptionHandlerTest {

    private val handler = UserApiExceptionHandler()

    @Test
    fun `비즈니스 예외를 ErrorCode의 응답으로 변환한다`() {
        val response = handler.handleBusiness(EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND))

        assertEquals(404, response.statusCode.value())
        assertEquals(ErrorResponse(404, "JOB_NOT_FOUND", "일자리 공고를 찾을 수 없습니다."), response.body)
    }

    @Test
    fun `검증 오류를 필드명 순서로 결합한다`() {
        val exception = BindException(ValidationTarget(), "request")
        exception.bindingResult.rejectValue("size", "Max", "100 이하여야 합니다.")
        exception.bindingResult.rejectValue("page", "Min", "0 이상이어야 합니다.")

        val response = handler.handleBind(exception)

        assertEquals(400, response.statusCode.value())
        assertEquals("BAD_REQUEST", response.body?.code)
        assertEquals("[page] 0 이상이어야 합니다., [size] 100 이하여야 합니다.", response.body?.message)
    }

    @Test
    fun `웹과 동시성 예외를 정해진 상태로 변환한다`() {
        val methodNotAllowed = handler.handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException("POST", listOf("GET")),
        )
        val noResource = handler.handleNoResourceFound(NoResourceFoundException(HttpMethod.GET, "/missing"))
        val optimistic = handler.handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException(Any::class.java, 1L),
        )

        assertEquals("METHOD_NOT_ALLOWED", methodNotAllowed.body?.code)
        assertEquals("API_NOT_FOUND", noResource.body?.code)
        assertEquals("요청한 API를 찾을 수 없습니다.", noResource.body?.message)
        assertEquals("OPTIMISTIC_LOCK_CONFLICT", optimistic.body?.code)
    }

    @Test
    fun `예상하지 못한 예외는 내부 메시지를 숨긴다`() {
        val response = handler.handleException(IllegalStateException("노출되면 안 되는 메시지"))

        assertEquals(500, response.statusCode.value())
        assertEquals("INTERNAL_SERVER_ERROR", response.body?.code)
        assertEquals("서버 내부 오류입니다.", response.body?.message)
        assertFalse(response.body?.message.orEmpty().contains("노출되면 안 되는 메시지"))
    }

    @Test
    fun `Security 오류도 동일한 JSON 계약을 사용한다`() {
        val objectMapper = ObjectMapper()
        val request = MockHttpServletRequest()
        val unauthorizedResponse = MockHttpServletResponse()
        val forbiddenResponse = MockHttpServletResponse()

        UserAuthenticationEntryPoint(objectMapper).commence(
            request,
            unauthorizedResponse,
            Mockito.mock(AuthenticationException::class.java),
        )
        UserAccessDeniedHandler(objectMapper).handle(
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
        var page: Int? = null,
        var size: Int? = null,
    )
}
