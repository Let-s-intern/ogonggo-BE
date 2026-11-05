package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.job.presentation.request.CreateProxyJobRequest
import com.ogonggo.adminapi.job.presentation.response.AdminJobDetailResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 채용공고")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminProxyJobApi {

    @Operation(
        operationId = "createCompanyMemberJob",
        summary = "비즈니스 회원 채용공고 대리 등록",
        description = """
            운영자가 비즈니스 회원 대신 채용공고를 등록합니다. 공고는 그 회원의 것(source = COMPANY)이 되어
            회원 마이페이지에 보이고, 회원이 직접 등록한 공고처럼 고칠 수 있습니다. 회원이 고치면 평소처럼 다시 검수를 기다립니다.

            요청 칸과 검증은 기업회원 채용공고 등록(사용자 API POST /api/v1/users/me/jobs)과 같고 visibility를 더합니다.
            검수 없이 승인(reviewStatus = APPROVED)으로 등록되며, visibility(기본 VISIBLE)로 바로 노출할지 정합니다.
            응답은 채용공고 상세 조회와 같으며 proxyRegisteredAt·proxyRegisteredByUserId에 대리 등록 일시와 관리자가 남습니다.
            로고·대표 이미지는 POST /api/v1/images로 올린 이미지 URL을 받습니다. 검수 요청 슬랙 알림은 보내지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 요청 칸이 올바르지 않습니다. 칸 사이 규칙 위반은 [필드명] 메시지로 알립니다. " +
                    "IMAGE_ASSET_NOT_AVAILABLE: 로고·대표 이미지 URL이 오공고에 올린 이미지인데 정리 작업이 이미 지웠거나 지우는 중입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "USER_NOT_FOUND: 회원이 없거나 비즈니스 회원이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "COMPANY_MEMBER_NOT_ACTIVE: 정지·탈퇴한 비즈니스 회원입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createCompanyMemberJob(
        @Parameter(description = "공고를 가질 비즈니스 회원 식별자") @Min(1) userId: Long,
        @Valid request: CreateProxyJobRequest,
        @Parameter(hidden = true) adminUserId: Long,
    ): ResponseEntity<SuccessResponse<AdminJobDetailResponse>>
}
