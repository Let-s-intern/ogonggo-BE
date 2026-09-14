package com.ogonggo.adminapi.review.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.adminapi.review.presentation.request.UpdateRejectionReasonRequest
import com.ogonggo.adminapi.review.presentation.response.AdminRejectionResponse
import com.ogonggo.core.review.domain.ReviewContentType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 반려 보관")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminRejectionApi {

    @Operation(
        operationId = "listRejections",
        summary = "반려 기록 목록 조회",
        description = """
            반려 중인 채용공고와 부트캠프의 반려 기록을 최근 반려 순으로 반환합니다.
            반려한 뒤 삭제된 콘텐츠도 남기며 contentExists=false로 표시합니다.
            keyword는 제목·회사명·사유에서 대소문자를 가리지 않고 부분 일치로 찾습니다.
            type(JOB·BOOTCAMP)을 보내지 않거나 빈 값을 보내면 두 종류를 모두 반환합니다.
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
    fun getRejections(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        @Size(max = 100) keyword: String?,
        type: ReviewContentType?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminRejectionResponse>>>

    @Operation(
        operationId = "updateRejection",
        summary = "반려 사유 수정",
        description = """
            반려 사유를 바꾸고 사유 수정 일시(reasonUpdatedAt)를 기록합니다. 사유를 비울 수는 없습니다.
            고친 사유를 기업회원에게 다시 전달하는 기능은 아직 없습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: type이 올바르지 않거나 사유가 비었습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "REJECTION_NOT_FOUND: 반려 기록을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateRejection(
        @Parameter(description = "job 또는 bootcamp") type: String,
        @Positive id: Long,
        @Valid request: UpdateRejectionReasonRequest,
    ): ResponseEntity<SuccessResponse<AdminRejectionResponse>>
}
