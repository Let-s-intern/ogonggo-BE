package com.ogonggo.core.concern.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class ConcernErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    CONCERN_NOT_FOUND(HttpStatus.NOT_FOUND, "고민글을 찾을 수 없습니다."),
    CONCERN_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "고민글을 수정하거나 삭제할 권한이 없습니다."),
}
