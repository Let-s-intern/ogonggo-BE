package com.ogonggo.core.adchannelformat.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdChannelFormatErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    AD_CHANNEL_FORMAT_NOT_FOUND(HttpStatus.NOT_FOUND, "게재 유형을 찾을 수 없습니다."),
}
