package com.ogonggo.core.calendartodo.error

import com.ogonggo.core.error.ErrorCode
import org.springframework.http.HttpStatus

enum class CalendarTodoErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {
    CALENDAR_TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "할 일을 찾을 수 없습니다."),
}
