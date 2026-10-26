package com.ogonggo.adminapi.notificationresult.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.notificationresult.presentation.response.AdminNotificationRecentResultsResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 알림")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminNotificationResultApi {
    @Operation(
        operationId = "getRecentNotificationResults",
        summary = "최근 알림 발송 결과 조회",
        description = "최근 생성된 알림 최대 100건을 생성 시각·ID 내림차순으로 반환합니다. 상태가 바뀌어도 생성 순서를 유지하며 수신 주소와 발송 payload는 포함하지 않습니다.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
        ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "FORBIDDEN: 관리자 권한 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ])
    fun getRecentNotificationResults(): ResponseEntity<SuccessResponse<AdminNotificationRecentResultsResponse>>
}
