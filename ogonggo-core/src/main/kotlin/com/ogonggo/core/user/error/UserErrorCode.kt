package com.ogonggo.core.user.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 사용자입니다."),
    USER_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴한 사용자입니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 사용자입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    COMPANY_PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 기업 정보가 등록된 사용자입니다."),
    COMPANY_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "기업 회원만 사용할 수 있습니다."),
}
