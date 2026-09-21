package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.presentation.request.ReplaceMyCompanyProfileRequest
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

@Tag(name = "내 정보")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface CompanyProfileApi {

    @Operation(
        operationId = "replaceMyCompanyProfile",
        summary = "내 기업 정보 수정",
        description = """
            기업 회원의 기관명과 담당자 이름을 교체합니다.
            조회는 내 정보 조회(GET /api/v1/users/me)의 companyProfile에 함께 담깁니다.

            두 값을 함께 교체하므로 하나만 바꿀 때도 바꾸지 않을 값을 함께 보내야 합니다.
            로그인 이메일과 비밀번호는 여기서 바꿀 수 없습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 기관명(150자)과 담당자 이름(100자)은 비어 있거나 길이를 넘을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "COMPANY_ROLE_REQUIRED, USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceMyCompanyProfile(
        @Parameter(hidden = true)
        userId: Long,
        @Valid
        request: ReplaceMyCompanyProfileRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
