package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.config.ADMIN_INTERNAL_API_KEY_SCHEME
import com.ogonggo.adminapi.job.presentation.request.CrawlerJobAnalysisRequest
import com.ogonggo.adminapi.job.presentation.response.CrawlerJobAnalysisTargetResponse
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
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

private const val UNAUTHORIZED_DESCRIPTION = "UNAUTHORIZED: 내부 API 키가 없거나 올바르지 않습니다."

@Tag(name = "크롤러 공고 분석")
@SecurityRequirement(name = ADMIN_INTERNAL_API_KEY_SCHEME)
interface CrawlerJobAnalysisApi {

    @Operation(
        operationId = "listCrawlerJobAnalysisTargets",
        summary = "공고 분석 대상 조회",
        description = """
            게시 중인 모집 중 채용공고 가운데 분석이 없거나 분석한 뒤로 본문이 바뀐 공고를 최근 것부터 줍니다.
            크롤러가 등록한 공고만이 아니라 고용24·기업회원 공고도 들어 있습니다.
            본문은 제목과 본문 여덟 칸이며, 응답의 `contentHash`를 분석을 보낼 때 그대로 돌려줍니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getAnalysisTargets(
        @Min(1) @Max(MAX_TARGETS.toLong()) size: Int,
    ): ResponseEntity<SuccessResponse<List<CrawlerJobAnalysisTargetResponse>>>

    @Operation(
        operationId = "replaceCrawlerJobAnalysis",
        summary = "공고 분석 저장",
        description = """
            크롤러가 만든 공고 분석을 저장합니다. 이미 있으면 바꿉니다.
            분석 대상을 받은 뒤 운영자가 본문을 고쳤으면 `contentHash`가 지금 본문과 달라 409로 거절합니다.
            그 공고는 다음 대상 조회에 다시 나옵니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "저장 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 요청 값이 올바르지 않거나 항목 개수를 넘었습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "JOB_NOT_FOUND: 공고가 없거나 지워졌습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "JOB_ANALYSIS_OUTDATED: 분석한 뒤로 공고 본문이 바뀌었습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceAnalysis(
        @Positive jobId: Long,
        @Valid request: CrawlerJobAnalysisRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    companion object {
        const val DEFAULT_TARGETS = 50
        const val MAX_TARGETS = 200
    }
}
