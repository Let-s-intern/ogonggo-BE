package com.ogonggo.adminapi.review.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.adminapi.review.presentation.request.DecideReviewRequest
import com.ogonggo.adminapi.review.presentation.response.AdminReviewDecisionResponse
import com.ogonggo.adminapi.review.presentation.response.AdminReviewItemResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

private const val TYPE_DESCRIPTION = "job 또는 bootcamp"
private const val CONTENT_NOT_FOUND_DESCRIPTION = "JOB_NOT_FOUND 또는 BOOTCAMP_NOT_FOUND"

@Tag(name = "관리자 검수 대기")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminReviewQueueApi {

    @Operation(
        operationId = "listReviewQueue",
        summary = "검수 대기 목록 조회",
        description = """
            기업회원이 올린 채용공고와 부트캠프 중 검수 대기인 것을 페이지 없이 모두 반환합니다.
            등록일이 오래된 순이며, 두 종류를 같은 모양(제목·회사·meta·sections·원문)으로 맞춰 줍니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 액세스 토큰이 없거나 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getQueue(): ResponseEntity<SuccessResponse<List<AdminReviewItemResponse>>>

    @Operation(
        operationId = "decideReview",
        summary = "검수 판정",
        description = """
            decision은 APPROVED 또는 REJECTED입니다. REJECTED에는 reason이 필요합니다.
            승인하면 곧바로 노출되고 반려 기록을 지웁니다. 반려하면 비노출로 두고 반려 기록을 남기며,
            이미 반려한 대상을 다시 반려하면 사유를 바꿉니다.
            반려 사유를 기업회원에게 전달하는 기능은 아직 없습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "판정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: type이나 decision이 올바르지 않거나 반려 사유가 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CONTENT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "CONTENT_NOT_REVIEWABLE: 크롤링 수집분은 검수 대상이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun decide(
        @Parameter(description = TYPE_DESCRIPTION) type: String,
        @Positive id: Long,
        @Valid request: DecideReviewRequest,
    ): ResponseEntity<SuccessResponse<AdminReviewDecisionResponse>>

    @Operation(
        operationId = "undoReview",
        summary = "검수 판정 되돌리기",
        description = "저장한 판정을 검수 대기로 되돌립니다. 비노출로 바뀌고 반려 기록을 지웁니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "되돌리기 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = CONTENT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "CONTENT_NOT_REVIEWABLE: 크롤링 수집분은 검수 대상이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun undo(
        @Parameter(description = TYPE_DESCRIPTION) type: String,
        @Positive id: Long,
    ): ResponseEntity<SuccessResponse<AdminReviewDecisionResponse>>
}
