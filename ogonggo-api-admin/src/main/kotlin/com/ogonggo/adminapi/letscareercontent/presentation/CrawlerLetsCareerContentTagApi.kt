package com.ogonggo.adminapi.letscareercontent.presentation

import com.ogonggo.adminapi.config.ADMIN_INTERNAL_API_KEY_SCHEME
import com.ogonggo.adminapi.letscareercontent.presentation.request.CrawlerLetsCareerContentTagRequest
import com.ogonggo.adminapi.letscareercontent.presentation.response.CrawlerLetsCareerContentTagTargetResponse
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

@Tag(name = "크롤러 렛츠커리어 콘텐츠 태그")
@SecurityRequirement(name = ADMIN_INTERNAL_API_KEY_SCHEME)
interface CrawlerLetsCareerContentTagApi {

    @Operation(
        operationId = "listCrawlerLetsCareerContentTagTargets",
        summary = "렛츠커리어 콘텐츠 태그 대상 조회",
        description = """
            공고 상세 추천에 쓰는 렛츠커리어 콘텐츠(프로그램·무료 자료집·블로그) 가운데
            태그가 없거나 태그한 뒤로 제목·설명이 바뀐 것을 줍니다. 응답의 `contentHash`를 태그를 보낼 때 그대로 돌려줍니다.
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
    fun getTagTargets(
        @Min(1) @Max(MAX_TARGETS.toLong()) size: Int,
    ): ResponseEntity<SuccessResponse<List<CrawlerLetsCareerContentTagTargetResponse>>>

    @Operation(
        operationId = "replaceCrawlerLetsCareerContentTags",
        summary = "렛츠커리어 콘텐츠 태그 저장",
        description = """
            크롤러가 붙인 직군·직무·준비 단계 태그를 저장합니다. 이미 있으면 바꿉니다.
            대상을 받은 뒤 렛츠커리어에서 내용이 바뀌었으면 `contentHash`가 달라 409로 거절하며, 그 콘텐츠는 다음 대상 조회에 다시 나옵니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "저장 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 요청 값이 올바르지 않거나 정의되지 않은 enum 값입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "LETS_CAREER_CONTENT_NOT_FOUND: 콘텐츠가 없거나 렛츠커리어 목록에서 빠졌습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "LETS_CAREER_CONTENT_TAGS_OUTDATED: 태그 대상을 받은 뒤로 콘텐츠 내용이 바뀌었습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceTags(
        @Positive contentId: Long,
        @Valid request: CrawlerLetsCareerContentTagRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    companion object {
        const val DEFAULT_TARGETS = 50
        const val MAX_TARGETS = 200
    }
}
