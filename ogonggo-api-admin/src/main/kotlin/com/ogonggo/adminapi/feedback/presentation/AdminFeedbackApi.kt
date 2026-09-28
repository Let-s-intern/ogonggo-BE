package com.ogonggo.adminapi.feedback.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.feedback.presentation.response.AdminFeedbackResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 개선 의견")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminFeedbackApi {

    @Operation(
        operationId = "listFeedbacks",
        summary = "개선 의견 목록 조회",
        description = "사용자가 남긴 개선 의견을 최근에 남긴 순으로 반환합니다. 필터와 정렬은 없습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 페이지 범위가 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getFeedbacks(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminFeedbackResponse>>>
}
