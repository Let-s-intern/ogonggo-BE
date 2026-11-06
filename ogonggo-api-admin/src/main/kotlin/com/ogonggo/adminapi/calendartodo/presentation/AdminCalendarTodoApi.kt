package com.ogonggo.adminapi.calendartodo.presentation

import com.ogonggo.adminapi.calendartodo.presentation.request.CreateAdminCalendarTodoRequest
import com.ogonggo.adminapi.calendartodo.presentation.request.UpdateAdminCalendarTodoRequest
import com.ogonggo.adminapi.calendartodo.presentation.response.AdminCalendarTodoIdResponse
import com.ogonggo.adminapi.calendartodo.presentation.response.AdminCalendarTodoResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

internal const val CALENDAR_RANGE_BAD_REQUEST_DESCRIPTION =
    "BAD_REQUEST: from, to: YYYY-MM-DD(KST) 로, from 이 to 보다 앞서게 보내 주세요. / to: from 부터 93일 안으로 보내 주세요."
private const val TODO_NOT_FOUND_DESCRIPTION = "CALENDAR_TODO_NOT_FOUND: 할 일을 찾을 수 없습니다."
private const val TODO_BAD_REQUEST_DESCRIPTION =
    "BAD_REQUEST: title: 1~100자로 보내 주세요. / content: null 이거나 2000자까지 보낼 수 있습니다. / " +
        "startDate·endDate: YYYY-MM-DD(KST) 로 보내 주세요. / endDate: 끝나는 날은 시작일과 같거나 뒤여야 합니다. / " +
        "done: 참·거짓 값을 보내 주세요."

@Tag(name = "관리자 캘린더 할 일")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminCalendarTodoApi {

    @Operation(
        operationId = "listCalendarTodos",
        summary = "캘린더 할 일 목록 조회",
        description = """
            날짜(시작·끝 포함)가 [from, to)와 겹치는(startDate < to 이고 endDate >= from) 할 일을 시작일, id 순으로 줍니다.
            from·to는 KST 날짜(YYYY-MM-DD)이고 from은 포함, to는 포함하지 않습니다. 기간은 93일까지입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = CALENDAR_RANGE_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getTodos(
        @Parameter(required = true, schema = Schema(type = "string", format = "date")) from: String?,
        @Parameter(required = true, schema = Schema(type = "string", format = "date")) to: String?,
    ): ResponseEntity<SuccessResponse<List<AdminCalendarTodoResponse>>>

    @Operation(operationId = "createCalendarTodo", summary = "캘린더 할 일 등록")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = TODO_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createTodo(
        @Valid request: CreateAdminCalendarTodoRequest,
    ): ResponseEntity<SuccessResponse<AdminCalendarTodoResponse>>

    @Operation(
        operationId = "updateCalendarTodo",
        summary = "캘린더 할 일 수정",
        description = "보낸 칸만 바꿉니다. 검증은 저장된 값과 합친 결과로 합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = TODO_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = TODO_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateTodo(
        @Positive todoId: Long,
        @Valid request: UpdateAdminCalendarTodoRequest,
    ): ResponseEntity<SuccessResponse<AdminCalendarTodoResponse>>

    @Operation(
        operationId = "deleteCalendarTodo",
        summary = "캘린더 할 일 삭제",
        description = "이미 지운 할 일을 다시 지워도 200입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = TODO_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteTodo(
        @Positive todoId: Long,
    ): ResponseEntity<SuccessResponse<AdminCalendarTodoIdResponse>>
}
