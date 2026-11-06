package com.ogonggo.adminapi.calendartodo.presentation

import com.ogonggo.adminapi.auth.business.AdminAuthService
import com.ogonggo.adminapi.auth.presentation.AdminAuthenticationFilter.Companion.ADMIN_AUTHORITY
import com.ogonggo.adminapi.calendartodo.business.AdminCalendarDateRange
import com.ogonggo.adminapi.calendartodo.business.AdminCalendarTodoResult
import com.ogonggo.adminapi.calendartodo.business.AdminCalendarTodoService
import com.ogonggo.adminapi.calendartodo.business.AdminCalendarTodoUpdateCommand
import com.ogonggo.adminapi.config.AdminSecurityConfiguration
import com.ogonggo.adminapi.error.AdminApiExceptionHandler
import com.ogonggo.core.calendartodo.error.CalendarTodoErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(controllers = [AdminCalendarTodoController::class])
@Import(AdminSecurityConfiguration::class, AdminApiExceptionHandler::class)
class AdminCalendarTodoControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockBean
    private lateinit var adminAuthService: AdminAuthService

    @MockBean
    private lateinit var adminCalendarTodoService: AdminCalendarTodoService

    @Test
    fun `기간의 할 일을 날짜 문자열로 준다`() {
        Mockito.`when`(adminCalendarTodoService.getTodos(AdminCalendarDateRange(FROM, FROM.plusDays(42)))).thenReturn(listOf(result()))

        mockMvc.perform(admin(get("/api/v1/admin/calendar-todos").param("from", "2026-10-26").param("to", "2026-12-07")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].startDate").value("2026-11-20"))
            .andExpect(jsonPath("$.data[0].endDate").value("2026-11-25"))
            .andExpect(jsonPath("$.data[0].content").isEmpty)
            .andExpect(jsonPath("$.data[0].createdAt").value("2026-11-04T14:30:00"))
    }

    @Test
    fun `기간이 빠지면 화면 문구로 400을 준다`() {
        mockMvc.perform(admin(get("/api/v1/admin/calendar-todos").param("from", "2026-10-26")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("from, to: YYYY-MM-DD(KST) 로, from 이 to 보다 앞서게 보내 주세요."))
    }

    @Test
    fun `수정은 보낸 칸만 넘기고 내용의 null은 지우기로 넘긴다`() {
        // given
        Mockito.`when`(adminCalendarTodoService.getTodo(TODO_ID)).thenReturn(result())

        // when & then
        mockMvc.perform(
            admin(patch("/api/v1/admin/calendar-todos/$TODO_ID"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"done":true,"content":null}"""),
        ).andExpect(status().isOk)
        Mockito.verify(adminCalendarTodoService)
            .updateTodo(TODO_ID, AdminCalendarTodoUpdateCommand(content = Optional.empty(), done = true))
    }

    @Test
    fun `없는 할 일은 404 CALENDAR_TODO_NOT_FOUND이고 지우면 id를 준다`() {
        Mockito.`when`(adminCalendarTodoService.getTodo(TODO_ID + 1))
            .thenThrow(EntityNotFoundException(CalendarTodoErrorCode.CALENDAR_TODO_NOT_FOUND))

        mockMvc.perform(
            admin(patch("/api/v1/admin/calendar-todos/${TODO_ID + 1}")).contentType(MediaType.APPLICATION_JSON).content("{}"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CALENDAR_TODO_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("할 일을 찾을 수 없습니다."))
        mockMvc.perform(admin(delete("/api/v1/admin/calendar-todos/$TODO_ID")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(TODO_ID))
    }

    private fun result() = AdminCalendarTodoResult(
        id = TODO_ID,
        title = "12월 홈 상단 배너 소재 받기",
        content = null,
        startDate = LocalDate.of(2026, 11, 20),
        endDate = LocalDate.of(2026, 11, 25),
        done = false,
        createdAt = LocalDateTime.of(2026, 11, 4, 14, 30),
        updatedAt = LocalDateTime.of(2026, 11, 4, 14, 30),
    )

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder = request.with(
        authentication(UsernamePasswordAuthenticationToken(ADMIN_ID, null, listOf(SimpleGrantedAuthority(ADMIN_AUTHORITY)))),
    )

    private companion object {
        const val ADMIN_ID = 9L
        const val TODO_ID = 1L
        val FROM: LocalDate = LocalDate.of(2026, 10, 26)
    }
}
