package com.ogonggo.core.calendartodo.implement

import com.ogonggo.core.calendartodo.domain.CalendarTodo
import com.ogonggo.core.calendartodo.implement.dto.CalendarTodoAppendDto
import com.ogonggo.core.calendartodo.persistence.CalendarTodoJpaRepository
import org.springframework.stereotype.Component

@Component
class CalendarTodoAppender internal constructor(
    private val calendarTodoRepository: CalendarTodoJpaRepository,
) {

    fun append(dto: CalendarTodoAppendDto): CalendarTodo = calendarTodoRepository.save(CalendarTodo(dto.terms))
}
