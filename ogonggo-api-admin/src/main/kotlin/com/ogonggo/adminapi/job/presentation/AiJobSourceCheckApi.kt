package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.config.ADMIN_INTERNAL_API_KEY_SCHEME
import com.ogonggo.adminapi.job.presentation.response.AiJobSourceCheckTargetResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

private const val UNAUTHORIZED_DESCRIPTION = "UNAUTHORIZED: 내부 API 키가 없거나 올바르지 않습니다."

@Tag(name = "AI 서버 상시 채용 원문 확인")
@SecurityRequirement(name = ADMIN_INTERNAL_API_KEY_SCHEME)
interface AiJobSourceCheckApi {

    @Operation(
        operationId = "listAiJobSourceCheckTargets",
        summary = "원문 확인 대상 조회",
        description = """
            게시 중인 모집 중 상시 채용 공고 가운데 원문 URL이 있는 것을 `afterId` 다음부터 식별자 순으로 `size`건 줍니다.
            AI 서버가 등록한 공고만이 아니라 고용24·기업회원 공고도 들어 있습니다.
            다음 쪽은 받은 마지막 `jobId`를 `afterId`로 넘겨 읽고, 빈 목록이면 끝입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: afterId가 음수이거나 size가 범위를 넘었습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getSourceCheckTargets(
        @Min(0) afterId: Long,
        @Min(1) @Max(MAX_TARGETS.toLong()) size: Int,
    ): ResponseEntity<SuccessResponse<List<AiJobSourceCheckTargetResponse>>>

    @Operation(
        operationId = "closeAiJobBySourceCheck",
        summary = "원문이 내려간 공고 마감",
        description = """
            원문 페이지가 없어졌거나 마감으로 바뀐 공고의 모집을 마감합니다. 등록 경로를 가리지 않습니다.
            게시 상태는 바꾸지 않아 사용자 화면에 모집 마감으로 보입니다.
            이미 마감한 공고를 다시 마감해도 처음 마감 일시를 그대로 두고 성공합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "마감 성공", useReturnTypeSchema = true),
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
        ],
    )
    fun closeBySourceCheck(
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    companion object {
        const val DEFAULT_TARGETS = 100
        const val MAX_TARGETS = 500
    }
}
