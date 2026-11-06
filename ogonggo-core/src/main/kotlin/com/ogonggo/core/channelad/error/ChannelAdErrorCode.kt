package com.ogonggo.core.channelad.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class ChannelAdErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    CHANNEL_AD_NOT_FOUND(HttpStatus.NOT_FOUND, "채널 광고를 찾을 수 없습니다."),
}
