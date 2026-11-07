package com.ogonggo.core.advertisementexposure.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdvertisementExposureErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    ADVERTISEMENT_EXPOSURE_NOT_FOUND(HttpStatus.NOT_FOUND, "광고 노출을 찾을 수 없습니다."),
    ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE(HttpStatus.BAD_REQUEST, "오공고 화면에 노출하지 않는 광고 상품입니다."),
    ADVERTISEMENT_BANNER_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "배너는 이름, 이미지, 링크만 있어야 합니다."),
    ADVERTISEMENT_JOB_POSTING_TARGET_REQUIRED(HttpStatus.BAD_REQUEST, "광고 공고는 띄울 공고만 있어야 합니다."),
    ADVERTISEMENT_EXPOSURE_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "노출 기간은 시작과 끝이 함께 있어야 하고 시작이 끝보다 앞서야 합니다."),
}
