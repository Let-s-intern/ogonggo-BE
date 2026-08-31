package com.ogonggo.core.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ErrorCodeTest {

    @Test
    fun `enum 이름을 안정적인 에러 코드로 사용한다`() {
        assertEquals("BAD_REQUEST", TestErrorCode.BAD_REQUEST.code)
    }

    @Test
    fun `enum이 아닌 ErrorCode는 UNKNOWN 코드를 사용한다`() {
        val errorCode = object : ErrorCode {
            override val httpStatus = HttpStatus.BAD_REQUEST
            override val message = "테스트 오류"
        }

        assertEquals("UNKNOWN", errorCode.code)
    }

    @Test
    fun `BusinessException은 전달받은 ErrorCode를 보존한다`() {
        val exception = BusinessException(TestErrorCode.CONFLICT)

        assertEquals(TestErrorCode.CONFLICT, exception.errorCode)
        assertEquals(TestErrorCode.CONFLICT.message, exception.message)
    }

    private enum class TestErrorCode(
        override val httpStatus: HttpStatus,
        override val message: String,
    ) : ErrorCode {
        BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
        CONFLICT(HttpStatus.CONFLICT, "충돌했습니다."),
    }
}
