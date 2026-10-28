package com.ogonggo.core.advertisementinquiry.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdvertisementInquiryErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    ADVERTISEMENT_INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "광고 문의를 찾을 수 없습니다."),
}
