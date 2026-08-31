package com.ogonggo.userapi.response

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.ResponseEntity

data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
) {
    fun toResponseEntity(): ResponseEntity<ErrorResponse> = ResponseEntity.status(status).body(this)

    companion object {
        fun from(errorCode: ErrorCode, message: String = errorCode.message): ErrorResponse = ErrorResponse(
            status = errorCode.httpStatus.value(),
            code = errorCode.code,
            message = message,
        )
    }
}
