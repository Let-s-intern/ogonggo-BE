package com.ogonggo.core.announcement.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AnnouncementErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
}
