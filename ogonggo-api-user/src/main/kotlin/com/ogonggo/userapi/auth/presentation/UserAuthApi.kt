package com.ogonggo.userapi.auth.presentation

import com.ogonggo.userapi.auth.presentation.request.LetsCareerSignInRequest
import com.ogonggo.userapi.auth.presentation.request.TokenReissueRequest
import com.ogonggo.userapi.auth.presentation.response.AccessTokenResponse
import com.ogonggo.userapi.auth.presentation.response.SignInResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
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

@Tag(name = "인증")
interface UserAuthApi {

    @Operation(
        operationId = "signInWithLetsCareer",
        summary = "렛츠커리어 토큰으로 로그인",
        description = "최초 로그인은 오공고 계정을 함께 생성하며 신규 여부는 isNewUser로 구분합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "401",
                description = "INVALID_LETSCAREER_TOKEN: 렛츠커리어 토큰이 유효하지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "503",
                description = "LETSCAREER_UNAVAILABLE: 렛츠커리어 서버와 통신할 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun signInWithLetsCareer(
        @Valid
        request: LetsCareerSignInRequest,
    ): ResponseEntity<SuccessResponse<SignInResponse>>

    @Operation(operationId = "reissueAccessToken", summary = "액세스 토큰 재발급")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "400",
                description = "NOT_REFRESH_TOKEN: 리프레시 토큰이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "EXPIRED_REFRESH_TOKEN: 만료되었거나 로그아웃된 리프레시 토큰입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun reissueAccessToken(
        @Valid
        request: TokenReissueRequest,
    ): ResponseEntity<SuccessResponse<AccessTokenResponse>>

    @Operation(operationId = "signOut", summary = "로그아웃")
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponse(
        responseCode = "401",
        description = "UNAUTHORIZED: 인증이 필요합니다.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun signOut(
        @Parameter(hidden = true)
        userId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
