package com.ogonggo.adminapi.calendartodo.presentation.response

import com.ogonggo.adminapi.calendartodo.business.AdminCalendarTodoResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminCalendarTodoResponse(
    val id: Long,
    val title: String,
    @Schema(description = "내용. 없으면 null입니다.")
    val content: String?,
    @Schema(description = "시작일(KST, 포함)")
    val startDate: LocalDate,
    @Schema(description = "끝나는 날(KST, 포함). 하루짜리는 startDate와 같습니다.")
    val endDate: LocalDate,
    val done: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminCalendarTodoResult): AdminCalendarTodoResponse = AdminCalendarTodoResponse(
            id = result.id,
            title = result.title,
            content = result.content,
            startDate = result.startDate,
            endDate = result.endDate,
            done = result.done,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
        )
    }
}

/** 지운 할 일의 식별자다. */
data class AdminCalendarTodoIdResponse(
    val id: Long,
)
