package com.ogonggo.adminapi.calendartodo.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.calendartodo.domain.CalendarTodo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDate
import java.util.Optional

class AdminCalendarTodoCommandsTest {

    @Test
    fun `등록은 목과 같은 순서로 보고 처음 걸린 칸 하나만 화면 문구로 알린다`() {
        assertMessage("title: 1~100자로 보내 주세요.", create(title = "   ", startDate = null))
        assertMessage("title: 1~100자로 보내 주세요.", create(title = "가".repeat(101)))
        assertMessage("content: null 이거나 2000자까지 보낼 수 있습니다.", create(content = "가".repeat(2001), startDate = null))
        assertMessage("startDate: YYYY-MM-DD(KST) 로 보내 주세요.", create(startDate = null, endDate = null))
        assertMessage("endDate: YYYY-MM-DD(KST) 로 보내 주세요.", create(endDate = null, done = null))
        assertMessage("endDate: 끝나는 날은 시작일과 같거나 뒤여야 합니다.", create(endDate = DAY.minusDays(1), done = null))
        assertMessage("done: 참·거짓 값을 보내 주세요.", create(done = null))
    }

    @Test
    fun `등록은 제목 앞뒤 공백을 빼고 빈 내용은 null로 저장한다`() {
        val terms = create(title = "  11월 광고 정산 ", content = "  ").toTerms()

        assertEquals("11월 광고 정산", terms.title)
        assertNull(terms.content)
    }

    @Test
    fun `수정은 보낸 칸만 바꾸고 저장된 값과 합친 결과로 끝나는 날을 본다`() {
        // given
        val todo = Mockito.mock(CalendarTodo::class.java)
        Mockito.`when`(todo.title).thenReturn("배너 소재 받기")
        Mockito.`when`(todo.content).thenReturn("PC·모바일 두 장")
        Mockito.`when`(todo.startDate).thenReturn(DAY)
        Mockito.`when`(todo.endDate).thenReturn(DAY.plusDays(5))
        Mockito.`when`(todo.done).thenReturn(false)

        // when
        val toggled = AdminCalendarTodoUpdateCommand(done = true).mergeInto(todo)
        val cleared = AdminCalendarTodoUpdateCommand(content = Optional.empty()).mergeInto(todo)

        // then
        assertEquals(listOf("배너 소재 받기", "PC·모바일 두 장", DAY, DAY.plusDays(5), true), toggled.values())
        assertNull(cleared.content)
        assertMessage(
            "endDate: 끝나는 날은 시작일과 같거나 뒤여야 합니다.",
            { AdminCalendarTodoUpdateCommand(startDate = DAY.plusDays(6)).mergeInto(todo) },
        )
    }

    @Test
    fun `조회 기간은 from 포함 to 미포함 날짜이고 빠지거나 틀리거나 앞뒤가 바뀌거나 93일을 넘으면 알린다`() {
        val rangeMessage = "from, to: YYYY-MM-DD(KST) 로, from 이 to 보다 앞서게 보내 주세요."

        assertEquals(AdminCalendarDateRange(DAY, DAY.plusDays(42)), AdminCalendarDateRange.parse("2026-11-20", "2027-01-01"))
        assertEquals(AdminCalendarDateRange(DAY, DAY.plusDays(93)), AdminCalendarDateRange.parse("2026-11-20", "2027-02-21"))
        assertMessage(rangeMessage, { AdminCalendarDateRange.parse(null, "2026-12-01") })
        assertMessage(rangeMessage, { AdminCalendarDateRange.parse("2026-11-31", "2026-12-01") })
        assertMessage(rangeMessage, { AdminCalendarDateRange.parse("2026-11-1", "2026-12-01") })
        assertMessage(rangeMessage, { AdminCalendarDateRange.parse("2026-12-01", "2026-12-01") })
        assertMessage("to: from 부터 93일 안으로 보내 주세요.", { AdminCalendarDateRange.parse("2026-11-20", "2027-02-22") })
    }

    private fun create(
        title: String? = "11월 광고 정산",
        content: String? = null,
        startDate: LocalDate? = DAY,
        endDate: LocalDate? = DAY,
        done: Boolean? = false,
    ) = AdminCalendarTodoCreateCommand(title = title, content = content, startDate = startDate, endDate = endDate, done = done)

    private fun assertMessage(message: String, command: AdminCalendarTodoCreateCommand) = assertMessage(message, { command.toTerms() })

    private fun assertMessage(message: String, block: () -> Unit) {
        assertEquals(message, assertThrows<AdminConsoleBadRequestException> { block() }.message)
    }

    private fun com.ogonggo.core.calendartodo.domain.CalendarTodoTerms.values() = listOf(title, content, startDate, endDate, done)

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 11, 20)
    }
}
