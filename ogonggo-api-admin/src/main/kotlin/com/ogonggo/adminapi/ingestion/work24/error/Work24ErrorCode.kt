package com.ogonggo.adminapi.ingestion.work24.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class Work24ErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    /** 사용 신청이 아직 승인되지 않았거나 설정에 키를 넣지 않은 서비스다. */
    WORK24_AUTH_KEY_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "고용24 인증키가 설정되지 않은 서비스입니다."),

    /** 고용24는 인증키·파라미터 오류도 200으로 응답하고 본문의 error에 사유를 담는다. 사유는 로그로 남긴다. */
    WORK24_REQUEST_REJECTED(HttpStatus.BAD_GATEWAY, "고용24가 요청을 거절했습니다. 요청 파라미터와 인증키를 확인해주세요."),
    WORK24_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "고용24 호출에 실패했습니다. 잠시 후 다시 시도해주세요."),
}
