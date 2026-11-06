package com.ogonggo.core.adchannel.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdChannelErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    AD_CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "채널을 찾을 수 없습니다."),
}
