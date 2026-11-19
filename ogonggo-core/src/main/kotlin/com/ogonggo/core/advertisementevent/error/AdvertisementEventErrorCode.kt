package com.ogonggo.core.advertisementevent.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdvertisementEventErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    ADVERTISEMENT_EVENT_EXPOSURE_NOT_SHOWN(HttpStatus.BAD_REQUEST, "현재 노출 중인 광고만 성과를 기록할 수 있습니다."),
    ADVERTISEMENT_REPORT_FORBIDDEN(HttpStatus.FORBIDDEN, "이 광고 계약의 리포트를 볼 권한이 없습니다."),
    ADVERTISEMENT_REPORT_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "리포트 기간은 최근 1년 이내의 날짜 범위여야 합니다."),
    ADVERTISEMENT_REPORT_SOURCE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "광고 성과 리포트 원천을 일시적으로 조회할 수 없습니다."),
}
