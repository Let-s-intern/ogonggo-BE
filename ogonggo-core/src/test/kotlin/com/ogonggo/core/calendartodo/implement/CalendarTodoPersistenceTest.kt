package com.ogonggo.core.calendartodo.implement

import com.ogonggo.core.calendartodo.domain.CalendarTodoTerms
import com.ogonggo.core.calendartodo.error.CalendarTodoErrorCode
import com.ogonggo.core.calendartodo.implement.dto.CalendarTodoAppendDto
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(CalendarTodoReader::class, CalendarTodoAppender::class, CalendarTodoManager::class)
internal class CalendarTodoPersistenceTest @Autowired constructor(
    private val reader: CalendarTodoReader,
    private val appender: CalendarTodoAppender,
    private val manager: CalendarTodoManager,
    private val entityManager: EntityManager,
) {

    @Test
    fun `기간과 겹치는 할 일을 시작일, id 순으로 읽고 끝나는 날이 조회 시작 전이거나 시작일이 조회 끝 이후면 뺀다`() {
        // given
        val endsOnFrom = append(start = FROM.minusDays(3), end = FROM)
        append(start = FROM.minusDays(3), end = FROM.minusDays(1))
        append(start = TO, end = TO)
        val startsBeforeTo = append(start = TO.minusDays(1), end = TO.plusDays(5))
        val sameStartLater = append(start = FROM.minusDays(3), end = FROM.plusDays(1))
        val deleted = append(start = FROM, end = FROM)
        manager.delete(reader.readForDelete(deleted), NOW)
        flushAndClear()

        // when
        val todos = reader.readAllOverlapping(FROM, TO)

        // then
        assertEquals(listOf(endsOnFrom, sameStartLater, startsBeforeTo), todos.map { it.id })
    }

    @Test
    fun `수정한 내용을 저장하고 지운 할 일은 다시 지울 수 있지만 조회·수정에서는 찾을 수 없다`() {
        // given
        val id = append(start = FROM, end = FROM)

        // when
        manager.change(reader.readForUpdate(id), CalendarTodoTerms("정산", "메모", FROM, FROM.plusDays(1), done = true))
        flushAndClear()
        val changed = reader.read(id)
        manager.delete(reader.readForDelete(id), NOW)
        manager.delete(reader.readForDelete(id), NOW.plusDays(1))
        flushAndClear()

        // then
        assertEquals(listOf("정산", "메모", FROM.plusDays(1), true), listOf(changed.title, changed.content, changed.endDate, changed.done))
        val exception = assertThrows(EntityNotFoundException::class.java) { reader.readForUpdate(id) }
        assertEquals(CalendarTodoErrorCode.CALENDAR_TODO_NOT_FOUND, exception.errorCode)
        assertEquals(NOW, reader.readForDelete(id).deletedAt)
    }

    private fun append(start: LocalDate, end: LocalDate): Long {
        val todo = appender.append(CalendarTodoAppendDto(CalendarTodoTerms("할 일", null, start, end, done = false)))
        entityManager.flush()
        return checkNotNull(todo.id)
    }

    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    private companion object {
        val FROM: LocalDate = LocalDate.of(2026, 10, 26)
        val TO: LocalDate = LocalDate.of(2026, 12, 7)
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
