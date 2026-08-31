package com.ogonggo.core.bootcamp.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class BootcampErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    BOOTCAMP_NOT_FOUND(HttpStatus.NOT_FOUND, "부트캠프를 찾을 수 없습니다."),
    INVALID_BOOTCAMP_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않는 부트캠프 상태 변경입니다."),
    BOOTCAMP_BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 북마크한 부트캠프입니다."),
}
