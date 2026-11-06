package com.ogonggo.core.calendartodo.implement

import com.ogonggo.core.calendartodo.domain.CalendarTodo
import com.ogonggo.core.calendartodo.domain.CalendarTodoTerms
import com.ogonggo.core.calendartodo.persistence.CalendarTodoJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CalendarTodoManager internal constructor(
    private val calendarTodoRepository: CalendarTodoJpaRepository,
) {

    fun change(todo: CalendarTodo, terms: CalendarTodoTerms) {
        todo.change(terms)
        calendarTodoRepository.save(todo)
    }

    fun delete(todo: CalendarTodo, now: LocalDateTime) {
        todo.delete(now)
        calendarTodoRepository.save(todo)
    }
}
