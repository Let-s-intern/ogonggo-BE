package com.ogonggo.userapi.auth.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    INVALID_LETSCAREER_TOKEN(HttpStatus.UNAUTHORIZED, "렛츠커리어 토큰이 유효하지 않습니다."),
    LETSCAREER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "렛츠커리어 서버와 통신할 수 없습니다."),
    INVALID_COMPANY_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."),
    NOT_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "리프레시 토큰이 아닙니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료되었거나 로그아웃된 리프레시 토큰입니다."),
}
