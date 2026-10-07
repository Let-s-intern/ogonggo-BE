package com.ogonggo.userapi.notification.fcm.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.notification.fcm.presentation.request.ReplaceFcmTokenRequest
import com.ogonggo.userapi.notification.fcm.presentation.response.FcmTokenResponse
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
interface FcmTokenApi {

    @Operation(
        operationId = "replaceMyFcmToken",
        summary = "내 FCM 토큰 저장",
        description = "로그인한 사용자의 현재 앱·브라우저 FCM 토큰을 저장합니다. 기존 토큰은 새 토큰으로 교체합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: FCM 토큰이 비어 있거나 너무 깁니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "USER_NOT_FOUND: 사용자를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceMyFcmToken(
        @Parameter(hidden = true)
        userId: Long,
        @Valid request: ReplaceFcmTokenRequest,
    ): ResponseEntity<SuccessResponse<FcmTokenResponse>>

    @Operation(operationId = "getMyFcmToken", summary = "내 FCM 토큰 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "USER_NOT_FOUND: 사용자를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getMyFcmToken(
        @Parameter(hidden = true)
        userId: Long,
    ): ResponseEntity<SuccessResponse<FcmTokenResponse>>
}
