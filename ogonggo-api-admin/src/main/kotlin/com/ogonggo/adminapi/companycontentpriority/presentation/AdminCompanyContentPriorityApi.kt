package com.ogonggo.adminapi.companycontentpriority.presentation

import com.ogonggo.adminapi.companycontentpriority.presentation.request.ReplaceAdminCompanyContentPriorityRequest
import com.ogonggo.adminapi.companycontentpriority.presentation.response.AdminCompanyContentPriorityResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 기업회원 공고 우선 노출")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminCompanyContentPriorityApi {

    @Operation(
        operationId = "getCompanyContentPriority",
        summary = "기업회원 공고 우선 노출 조회",
        description = """
            기업회원이 비즈니스로 등록한 마감 전 채용공고·부트캠프를 공개 목록(`GET /api/v1/jobs`, `GET /api/v1/bootcamps`) 맨 앞에 두는지 줍니다.
            바꾼 적이 없으면 켜짐(`enabled: true`)입니다.
        """,
    )
    fun getPriority(): ResponseEntity<SuccessResponse<AdminCompanyContentPriorityResponse>>

    @Operation(
        operationId = "replaceCompanyContentPriority",
        summary = "기업회원 공고 우선 노출 수정",
        description = """
            우선 노출을 켜거나 끄고 바뀐 값을 반환합니다. 다음 공개 목록 요청부터 바로 씁니다. 같은 요청을 반복해도 결과가 같습니다.
            끄면 기업회원 공고는 다른 공고와 같은 기준으로 정렬되고, 부트캠프는 기업 연계 과정만 먼저 둡니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: enabled가 빠졌거나 참·거짓이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replacePriority(
        @Valid request: ReplaceAdminCompanyContentPriorityRequest,
    ): ResponseEntity<SuccessResponse<AdminCompanyContentPriorityResponse>>
}
