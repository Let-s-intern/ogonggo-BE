package com.ogonggo.adminapi.calendartodo.presentation

import com.ogonggo.adminapi.calendartodo.business.AdminCalendarDateRange
import com.ogonggo.adminapi.calendartodo.business.AdminCalendarTodoService
import com.ogonggo.adminapi.calendartodo.presentation.request.CreateAdminCalendarTodoRequest
import com.ogonggo.adminapi.calendartodo.presentation.request.UpdateAdminCalendarTodoRequest
import com.ogonggo.adminapi.calendartodo.presentation.response.AdminCalendarTodoIdResponse
import com.ogonggo.adminapi.calendartodo.presentation.response.AdminCalendarTodoResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/calendar-todos")
class AdminCalendarTodoController(
    private val adminCalendarTodoService: AdminCalendarTodoService,
) : AdminCalendarTodoApi {

    /** 빠지거나 틀린 날짜를 화면 문구로 알리려고 문자열로 받는다. 날짜 바인딩에 맡기면 기본 400 문구가 나간다. */
    @GetMapping
    override fun getTodos(
        @RequestParam(name = "from", required = false) from: String?,
        @RequestParam(name = "to", required = false) to: String?,
    ): ResponseEntity<SuccessResponse<List<AdminCalendarTodoResponse>>> =
        SuccessResponse.ok(
            adminCalendarTodoService.getTodos(AdminCalendarDateRange.parse(from, to)).map(AdminCalendarTodoResponse::from),
        )

    /** 등록이 커밋된 뒤의 할 일을 다시 읽어 응답한다. */
    @PostMapping
    override fun createTodo(
        @RequestBody request: CreateAdminCalendarTodoRequest,
    ): ResponseEntity<SuccessResponse<AdminCalendarTodoResponse>> {
        val todoId = adminCalendarTodoService.createTodo(request.toCommand())
        return SuccessResponse.created(AdminCalendarTodoResponse.from(adminCalendarTodoService.getTodo(todoId)))
    }

    /** 수정이 커밋된 뒤의 할 일을 다시 읽어 응답한다. */
    @PatchMapping("/{todoId}")
    override fun updateTodo(
        @PathVariable("todoId") todoId: Long,
        @RequestBody request: UpdateAdminCalendarTodoRequest,
    ): ResponseEntity<SuccessResponse<AdminCalendarTodoResponse>> {
        adminCalendarTodoService.updateTodo(todoId, request.toCommand())
        return SuccessResponse.ok(AdminCalendarTodoResponse.from(adminCalendarTodoService.getTodo(todoId)))
    }

    @DeleteMapping("/{todoId}")
    override fun deleteTodo(
        @PathVariable("todoId") todoId: Long,
    ): ResponseEntity<SuccessResponse<AdminCalendarTodoIdResponse>> {
        adminCalendarTodoService.deleteTodo(todoId)
        return SuccessResponse.ok(AdminCalendarTodoIdResponse(todoId))
    }
}
