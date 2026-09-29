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
    USER_PROFILE_CONFLICT(HttpStatus.CONFLICT, "프로필 저장이 동시에 요청되었습니다. 다시 시도해 주세요."),
    GENERAL_MEMBER_REQUIRED(HttpStatus.FORBIDDEN, "일반 회원만 사용할 수 있습니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "기존 비밀번호가 일치하지 않습니다."),
    INVALID_NEW_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호는 8자 이상이며 특수문자를 하나 이상 포함해야 합니다."),
    SOCIAL_ACCOUNT_PASSWORD_UNAVAILABLE(HttpStatus.BAD_REQUEST, "소셜 로그인으로 가입한 계정은 비밀번호를 변경할 수 없습니다."),
}
