package com.ogonggo.adminapi.notificationbatch.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdminNotificationBatchErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    /** 광고성 발송은 동의자에게만 보내야 하므로, 동의 여부를 모르면 발송을 등록하지 않는다. */
    MARKETING_AGREEMENT_UNAVAILABLE(
        HttpStatus.SERVICE_UNAVAILABLE,
        "렛츠커리어에서 마케팅 수신 동의 여부를 확인하지 못해 광고성 발송을 등록하지 않았습니다. 잠시 후 다시 시도해주세요.",
    ),
}
