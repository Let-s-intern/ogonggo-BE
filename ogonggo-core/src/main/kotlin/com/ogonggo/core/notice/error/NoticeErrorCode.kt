package com.ogonggo.core.notice.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class NoticeErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
}
