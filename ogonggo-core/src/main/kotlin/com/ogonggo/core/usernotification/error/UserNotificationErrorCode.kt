package com.ogonggo.core.usernotification.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class UserNotificationErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    USER_NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
}
