package com.ogonggo.core.community.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class RecruitmentPostErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    RECRUITMENT_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "모집글을 찾을 수 없습니다."),
}
