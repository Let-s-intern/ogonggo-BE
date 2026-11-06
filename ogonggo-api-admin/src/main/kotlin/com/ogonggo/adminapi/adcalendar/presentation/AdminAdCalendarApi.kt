package com.ogonggo.adminapi.adcalendar.presentation

import com.ogonggo.adminapi.adcalendar.presentation.response.AdminAdCalendarResponse
import com.ogonggo.adminapi.calendartodo.presentation.CALENDAR_RANGE_BAD_REQUEST_DESCRIPTION
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
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 광고 일정")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdCalendarApi {

    @Operation(
        operationId = "getAdCalendar",
        summary = "광고 일정 조회",
        description = """
            from·to는 KST 날짜(YYYY-MM-DD)이고 from은 포함, to는 포함하지 않습니다. 기간은 93일까지입니다.
            계약은 startAt < to 00:00 이고 endAt > from 00:00 인 것, 배너·광고 공고는 같은 조건에서 startAt·endAt이 null이면
            그쪽이 열린 것으로 보고 겹치는 것(꺼진 것 포함), 채널 광고는 postDate ~ endDate(없으면 postDate, 둘 다 포함)가
            [from, to)와 겹치는 것(취소한 것 포함)을 줍니다.
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
    fun getAdCalendar(
        @Parameter(required = true, schema = Schema(type = "string", format = "date")) from: String?,
        @Parameter(required = true, schema = Schema(type = "string", format = "date")) to: String?,
    ): ResponseEntity<SuccessResponse<AdminAdCalendarResponse>>
}
