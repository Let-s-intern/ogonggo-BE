package com.ogonggo.core.advertisercontract.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdvertiserContractErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    ADVERTISER_CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "계약을 찾을 수 없습니다."),
}
