package com.ogonggo.adminapi.notification.intake.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdminWelcomeEmailErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    WELCOME_EMAIL_RECIPIENT_UNAVAILABLE(HttpStatus.CONFLICT, "가입 이메일이 없어 환영 메일을 재발송할 수 없습니다."),
}
