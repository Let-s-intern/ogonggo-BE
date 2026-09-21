package com.ogonggo.core.community.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class RecruitmentPostApplicationErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    RECRUITMENT_POST_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "지원 이력을 찾을 수 없습니다."),
    INVALID_RECRUITMENT_APPLICATION_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않는 지원 단계 변경입니다."),
}
