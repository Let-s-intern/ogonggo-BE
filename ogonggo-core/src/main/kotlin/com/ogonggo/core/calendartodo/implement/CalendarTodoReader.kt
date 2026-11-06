package com.ogonggo.core.calendartodo.implement

import com.ogonggo.core.calendartodo.domain.CalendarTodo
import com.ogonggo.core.calendartodo.error.CalendarTodoErrorCode
import com.ogonggo.core.calendartodo.persistence.CalendarTodoJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class CalendarTodoReader internal constructor(
    private val calendarTodoRepository: CalendarTodoJpaRepository,
) {

    fun read(todoId: Long): CalendarTodo =
        calendarTodoRepository.findByIdAndDeletedAtIsNull(todoId)
            ?: throw EntityNotFoundException(CalendarTodoErrorCode.CALENDAR_TODO_NOT_FOUND)

    /** 날짜(시작·끝 포함)가 `[from, toExclusive)`와 겹치는 할 일이다. 시작일, 식별자 순이다. */
    fun readAllOverlapping(from: LocalDate, toExclusive: LocalDate): List<CalendarTodo> =
        calendarTodoRepository.findAllOverlapping(from, toExclusive)

    /** 수정할 할 일을 잠가 읽는다. 호출한 서비스의 트랜잭션 안에서 부른다. */
    fun readForUpdate(todoId: Long): CalendarTodo =
        calendarTodoRepository.findForUpdate(todoId)
            ?: throw EntityNotFoundException(CalendarTodoErrorCode.CALENDAR_TODO_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 할 일도 잠가 찾는다. */
    fun readForDelete(todoId: Long): CalendarTodo =
        calendarTodoRepository.findIncludingDeletedForUpdate(todoId)
            ?: throw EntityNotFoundException(CalendarTodoErrorCode.CALENDAR_TODO_NOT_FOUND)
}
