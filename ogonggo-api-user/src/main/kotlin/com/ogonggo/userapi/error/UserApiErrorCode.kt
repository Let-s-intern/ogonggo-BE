package com.ogonggo.userapi.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class UserApiErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "리소스 접근 권한이 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "리소스 접근 권한이 없습니다."),
    API_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 API를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "잘못된 HTTP method 요청입니다."),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "다른 요청에서 리소스가 변경되었습니다. 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
}
