package com.ogonggo.core.banner.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class BannerErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "배너를 찾을 수 없습니다."),
}
