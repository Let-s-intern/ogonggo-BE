package com.ogonggo.adminapi.calendartodo.business

import com.ogonggo.core.calendartodo.implement.CalendarTodoAppender
import com.ogonggo.core.calendartodo.implement.CalendarTodoManager
import com.ogonggo.core.calendartodo.implement.CalendarTodoReader
import com.ogonggo.core.calendartodo.implement.dto.CalendarTodoAppendDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/** 관리자 콘솔 캘린더의 운영자 할 일이다. 운영자 모두가 같은 목록을 본다. */
@Service
class AdminCalendarTodoService(
    private val calendarTodoReader: CalendarTodoReader,
    private val calendarTodoAppender: CalendarTodoAppender,
    private val calendarTodoManager: CalendarTodoManager,
    private val clock: Clock,
) {

    /** 날짜가 [range]와 겹치는 할 일이다. 시작일, 식별자 순이다. */
    fun getTodos(range: AdminCalendarDateRange): List<AdminCalendarTodoResult> =
        calendarTodoReader.readAllOverlapping(range.from, range.toExclusive).map(AdminCalendarTodoResult::of)

    fun getTodo(todoId: Long): AdminCalendarTodoResult = AdminCalendarTodoResult.of(calendarTodoReader.read(todoId))

    /** 할 일을 등록하고 식별자를 돌려준다. */
    @Transactional
    fun createTodo(command: AdminCalendarTodoCreateCommand): Long =
        calendarTodoAppender.append(CalendarTodoAppendDto(command.toTerms())).requiredId()

    /** 보낸 칸만 바꾼다. 검증은 저장된 값과 합친 결과로 한다. */
    @Transactional
    fun updateTodo(todoId: Long, command: AdminCalendarTodoUpdateCommand) {
        val todo = calendarTodoReader.readForUpdate(todoId)
        calendarTodoManager.change(todo, command.mergeInto(todo))
    }

    /** 이미 지운 할 일을 다시 지워도 성공한다. */
    @Transactional
    fun deleteTodo(todoId: Long) {
        calendarTodoManager.delete(calendarTodoReader.readForDelete(todoId), LocalDateTime.now(clock))
    }
}
