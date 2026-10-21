package com.ogonggo.core.notificationbatch.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class NotificationBatchErrorCode(override val httpStatus: HttpStatus, override val message: String) : ErrorCode {
    NOTIFICATION_BATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "발송 묶음을 찾을 수 없거나 결과 조회 기간이 지났습니다."),
}
