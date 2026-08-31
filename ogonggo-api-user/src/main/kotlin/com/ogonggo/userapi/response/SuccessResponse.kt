package com.ogonggo.userapi.response

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

data class SuccessResponse<T> private constructor(
    val status: Int,
    val message: String,
    val data: T?,
) {
    companion object {
        private const val SUCCESS_MESSAGE = "요청이 성공했습니다."

        fun <T> ok(data: T): ResponseEntity<SuccessResponse<T>> =
            of(HttpStatus.OK, data)

        fun ok(): ResponseEntity<SuccessResponse<Unit>> =
            of(HttpStatus.OK, null)

        fun <T> created(data: T): ResponseEntity<SuccessResponse<T>> =
            of(HttpStatus.CREATED, data)

        fun created(): ResponseEntity<SuccessResponse<Unit>> =
            of(HttpStatus.CREATED, null)

        private fun <T> of(
            httpStatus: HttpStatus,
            data: T?,
        ): ResponseEntity<SuccessResponse<T>> = ResponseEntity.status(httpStatus).body(
            SuccessResponse(
                status = httpStatus.value(),
                message = SUCCESS_MESSAGE,
                data = data,
            ),
        )
    }
}
