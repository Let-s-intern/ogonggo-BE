package com.ogonggo.userapi.notification.fcm.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.notification.fcm.presentation.request.SendFcmTestNotificationRequest
import com.ogonggo.userapi.notification.fcm.presentation.response.FcmTestNotificationResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "푸시 알림")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface FcmTestNotificationApi {

    @Operation(
        operationId = "sendMyFcmTestNotification",
        summary = "내 FCM 테스트 알림 적재",
        description = "로그인한 사용자의 저장된 FCM 토큰을 대상으로 notification을 적재합니다. 실제 발송은 dispatcher가 비동기로 수행합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "notification 적재 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 제목·본문 또는 data가 올바르지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "FCM_TOKEN_NOT_REGISTERED: FCM 토큰이 등록되지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun sendMyFcmTestNotification(
        @Parameter(hidden = true)
        userId: Long,
        @Valid request: SendFcmTestNotificationRequest,
    ): ResponseEntity<SuccessResponse<FcmTestNotificationResponse>>
}
