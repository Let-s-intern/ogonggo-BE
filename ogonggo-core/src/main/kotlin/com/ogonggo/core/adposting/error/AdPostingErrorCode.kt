package com.ogonggo.core.adposting.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AdPostingErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    AD_POSTING_NOT_FOUND(HttpStatus.NOT_FOUND, "광고 공고를 찾을 수 없습니다."),
}
