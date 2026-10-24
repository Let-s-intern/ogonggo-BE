package com.ogonggo.userapi.usernotification.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.usernotification.presentation.response.UserNotificationPageResponse
import com.ogonggo.userapi.usernotification.presentation.response.UserNotificationUnreadCountResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

@Tag(name = "사용자 알림함")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface UserNotificationApi {

    @Operation(operationId = "listMyNotifications", summary = "내 알림 목록 조회", description = "최근 30일 개인 알림과 가입 이후 전체 공지를 최신순으로 조회합니다. source=PERSONAL은 숫자 id, BROADCAST는 batchId로 읽음 처리합니다. 같은 시각은 공통 공지, 개인 알림 순이며 각 종류 안에서 ID 내림차순입니다. 관리자 알림은 ADMIN_NOTIFICATION이며 postId·commentId는 null, 이동 주소는 url입니다. 토큰·푸시 결과와 무관하게 보관합니다. nextCursor를 그대로 다음 요청에 사용합니다. 기본 20건, 최대 100건입니다.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "BAD_REQUEST: 잘못된 cursor·size 또는 식별자", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "USER_SUSPENDED 또는 USER_WITHDRAWN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "USER_NOT_FOUND", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ])
    fun listMyNotifications(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "직전 응답의 nextCursor. 첫 조회는 생략") cursor: String?,
        @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<UserNotificationPageResponse>>

    @Operation(operationId = "getMyNotificationUnreadCount", summary = "내 안 읽은 알림 개수 조회", description = "최근 30일의 실제 개수를 반환합니다. 99+ 표시는 클라이언트에서 처리합니다.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
        ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "USER_SUSPENDED 또는 USER_WITHDRAWN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "USER_NOT_FOUND", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ])
    fun getMyNotificationUnreadCount(
        @Parameter(hidden = true) userId: Long,
    ): ResponseEntity<SuccessResponse<UserNotificationUnreadCountResponse>>

    @Operation(operationId = "readMyNotification", summary = "내 알림 한 건 읽음", description = "최초 읽은 시각만 보존합니다. 반복 호출은 성공하며, 없는 알림·타인 알림·30일이 지난 알림은 404입니다.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "OK: data는 null입니다.", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "BAD_REQUEST: 잘못된 cursor·size 또는 식별자", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "USER_SUSPENDED 또는 USER_WITHDRAWN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "USER_NOT_FOUND 또는 USER_NOTIFICATION_NOT_FOUND", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ])
    fun readMyNotification(
        @Parameter(hidden = true) userId: Long,
        @Positive notificationId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(operationId = "readMyBroadcastNotification", summary = "내 전체 공지 한 건 읽음", description = "가입 이후 게시된 최근 30일 공지의 최초 읽음 시각을 저장합니다. source=BROADCAST의 batchId를 사용하며 반복 호출도 성공합니다.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "BAD_REQUEST: 잘못된 UUID", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "USER_SUSPENDED 또는 USER_WITHDRAWN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "USER_NOT_FOUND 또는 USER_NOTIFICATION_NOT_FOUND", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ])
    fun readMyBroadcastNotification(@Parameter(hidden = true) userId: Long, batchId: java.util.UUID): ResponseEntity<SuccessResponse<Unit>>

    @Operation(operationId = "readAllMyNotifications", summary = "내 알림 모두 읽음", description = "개인 알림은 요청 시각·ID 상한까지, 공통 공지는 요청 시각까지 커밋되어 보이는 가입 이후 최근 30일 항목을 읽음 처리합니다. 본문 없이 호출합니다.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "OK: data는 null입니다.", useReturnTypeSchema = true),
        ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "USER_SUSPENDED 또는 USER_WITHDRAWN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "USER_NOT_FOUND", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ])
    fun readAllMyNotifications(
        @Parameter(hidden = true) userId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
