package com.ogonggo.core.job.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class JobErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "일자리 공고를 찾을 수 없습니다."),
    JOB_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 일자리 공고입니다."),
    JOB_BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 북마크한 일자리 공고입니다."),
    JOB_BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "북마크하지 않은 일자리 공고입니다."),
    INVALID_JOB_APPLICATION_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않는 지원 단계 변경입니다."),
    JOB_ARCHIVED(HttpStatus.CONFLICT, "보관된 채용공고는 변경할 수 없습니다."),
}
