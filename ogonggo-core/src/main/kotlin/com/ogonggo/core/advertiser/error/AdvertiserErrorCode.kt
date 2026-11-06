package com.ogonggo.core.advertiser.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdvertiserErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    ADVERTISER_NOT_FOUND(HttpStatus.NOT_FOUND, "광고주를 찾을 수 없습니다."),
}
