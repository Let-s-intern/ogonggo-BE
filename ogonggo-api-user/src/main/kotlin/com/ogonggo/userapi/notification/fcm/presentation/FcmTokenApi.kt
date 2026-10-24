package com.ogonggo.userapi.notification.fcm.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.notification.fcm.presentation.request.ReplaceFcmTokenRequest
import com.ogonggo.userapi.notification.fcm.presentation.request.DeleteFcmTokenRequest
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
        description = "설치별 deviceId(UUID)의 토큰만 등록·교체합니다. deviceId 생략 또는 null은 회원별 기존 클라이언트 슬롯만 교체합니다. " +
            "같은 설치 또는 토큰의 이전 계정 연결은 해제합니다. 동시 등록 충돌 시 요청 전체를 재시도합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 토큰은 1~4096자이며 공백만 보낼 수 없습니다. deviceId는 UUID 형식이어야 합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "FCM_TOKEN_CONFLICT: 동시 등록 충돌. 요청 전체를 재시도합니다.",
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

    @Operation(
        operationId = "getMyFcmToken",
        summary = "내 FCM 토큰 조회",
        description = "가장 최근 등록·갱신한 활성 토큰 한 개를 반환합니다. 없으면 token은 null입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
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

    @Operation(
        operationId = "deleteMyFcmToken",
        summary = "내 FCM 토큰 해제",
        description = "로그인한 회원의 일치하는 활성 토큰만 해제합니다. 이미 해제됐거나 다른 회원의 토큰이어도 200입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK: data는 null입니다.", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 비어 있거나 4096자를 넘는 토큰",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "USER_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteMyFcmToken(
        @Parameter(hidden = true) userId: Long,
        @Valid request: DeleteFcmTokenRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
