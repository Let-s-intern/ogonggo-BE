package com.ogonggo.adminapi.calendartodo.business

import com.ogonggo.core.calendartodo.domain.CalendarTodo
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminCalendarTodoResult(
    val id: Long,
    val title: String,
    val content: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val done: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun of(todo: CalendarTodo): AdminCalendarTodoResult = AdminCalendarTodoResult(
            id = todo.requiredId(),
            title = todo.title,
            content = todo.content,
            startDate = todo.startDate,
            endDate = todo.endDate,
            done = todo.done,
            createdAt = todo.createdAt,
            updatedAt = todo.updatedAt,
        )
    }
}

internal fun CalendarTodo.requiredId(): Long = checkNotNull(id) { "할 일 식별자가 없습니다." }
