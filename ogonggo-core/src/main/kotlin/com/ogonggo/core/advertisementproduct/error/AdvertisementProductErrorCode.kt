package com.ogonggo.core.advertisementproduct.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdvertisementProductErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    ADVERTISEMENT_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "광고 상품을 찾을 수 없습니다."),
    ADVERTISEMENT_PRODUCT_POSITION_MISMATCH(HttpStatus.BAD_REQUEST, "노출 자리가 상품 카테고리와 맞지 않습니다."),
}
