package com.ogonggo.core.calendartodo.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class CalendarTodoDomainTest {

    @Test
    fun `제목과 내용은 앞뒤 공백을 빼고 빈 내용은 null로 맞춘다`() {
        val terms = terms(title = "  11월 광고 정산 ", content = "   ")

        assertEquals("11월 광고 정산", terms.title)
        assertNull(terms.content)
    }

    @Test
    fun `하루짜리는 시작일과 끝나는 날이 같고 끝나는 날이 시작일보다 앞이면 만들 수 없다`() {
        terms(startDate = DAY, endDate = DAY)

        assertThrows<IllegalArgumentException> { terms(startDate = DAY, endDate = DAY.minusDays(1)) }
    }

    @Test
    fun `지운 할 일은 고칠 수 없고 다시 지워도 처음 삭제 일시를 지킨다`() {
        // given
        val todo = CalendarTodo(terms())
        val now = LocalDateTime.of(2026, 11, 6, 10, 0)

        // when
        todo.delete(now)
        todo.delete(now.plusDays(1))

        // then
        assertEquals(now, todo.deletedAt)
        assertThrows<IllegalStateException> { todo.change(terms(done = true)) }
    }

    private fun terms(
        title: String = "배너 소재 받기",
        content: String? = null,
        startDate: LocalDate = DAY,
        endDate: LocalDate = DAY.plusDays(2),
        done: Boolean = false,
    ) = CalendarTodoTerms(title = title, content = content, startDate = startDate, endDate = endDate, done = done)

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 11, 20)
    }
}
