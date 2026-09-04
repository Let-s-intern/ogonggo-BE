package com.ogonggo.userapi.advertisement.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdvertisementErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    /**
     * 문의를 저장하지 않고 슬랙으로만 전달하므로, 발송에 실패하면 접수도 실패다.
     * 200을 돌려주면 문의가 사라졌는데 접수됐다고 알리게 된다.
     */
    ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED(
        HttpStatus.SERVICE_UNAVAILABLE,
        "광고 문의 접수에 실패했습니다. 잠시 후 다시 시도해주세요.",
    ),
}
