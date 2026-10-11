package com.ogonggo.userapi.notification.fcm.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class FcmNotificationErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    FCM_TOKEN_NOT_REGISTERED(HttpStatus.NOT_FOUND, "FCM 토큰이 등록되지 않았습니다."),
}
