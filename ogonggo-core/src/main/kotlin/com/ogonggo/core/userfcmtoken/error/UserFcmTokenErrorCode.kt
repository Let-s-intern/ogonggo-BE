package com.ogonggo.core.userfcmtoken.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class UserFcmTokenErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    FCM_TOKEN_CONFLICT(HttpStatus.CONFLICT, "FCM 토큰 등록이 동시에 요청되었습니다. 다시 시도해 주세요."),
}
