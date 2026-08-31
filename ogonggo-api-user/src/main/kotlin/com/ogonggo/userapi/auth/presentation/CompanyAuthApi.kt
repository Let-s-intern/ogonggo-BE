package com.ogonggo.userapi.auth.presentation

import com.ogonggo.userapi.auth.presentation.request.CompanySignInRequest
import com.ogonggo.userapi.auth.presentation.request.CompanySignUpRequest
import com.ogonggo.userapi.auth.presentation.response.AuthTokenResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "기업 인증")
@RequestMapping("/api/v1/auth/company")
interface CompanyAuthApi {

    @Operation(
        summary = "기업 회원가입",
        description = "렛츠커리어를 거치지 않고 오공고 계정을 만듭니다. 승인 절차가 없어 가입 즉시 세션을 발급합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "가입 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "409",
                description = "EMAIL_ALREADY_EXISTS: 이미 사용 중인 이메일입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/signup")
    fun signUp(
        @RequestBody
        @Valid
        request: CompanySignUpRequest,
    ): ResponseEntity<SuccessResponse<AuthTokenResponse>>

    @Operation(summary = "기업 로그인")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "INVALID_COMPANY_CREDENTIALS: 이메일 또는 비밀번호가 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/signin")
    fun signIn(
        @RequestBody
        @Valid
        request: CompanySignInRequest,
    ): ResponseEntity<SuccessResponse<AuthTokenResponse>>
}
