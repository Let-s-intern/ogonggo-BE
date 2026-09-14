package com.ogonggo.core.review.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class ReviewErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    CONTENT_NOT_REVIEWABLE(HttpStatus.CONFLICT, "크롤링으로 수집한 콘텐츠는 검수 대상이 아닙니다."),
    REVIEW_NOT_APPROVED(HttpStatus.CONFLICT, "검수 승인 전에는 노출할 수 없습니다."),
    REJECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "반려 기록을 찾을 수 없습니다."),
}
