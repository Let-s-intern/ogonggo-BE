package com.ogonggo.userapi.concern.presentation

import com.ogonggo.userapi.concern.presentation.request.ConcernListRequest
import com.ogonggo.userapi.concern.presentation.request.SaveConcernRequest
import com.ogonggo.userapi.concern.presentation.response.ConcernDetailResponse
import com.ogonggo.userapi.concern.presentation.response.ConcernSummaryResponse
import com.ogonggo.userapi.concern.presentation.response.CreateConcernResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.PageResponse
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
import jakarta.validation.constraints.Positive
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity

@Tag(name = "취준고민")
interface ConcernApi {

    @Operation(
        operationId = "listPublicConcerns",
        summary = "고민글 목록 조회",
        description = """
            삭제되지 않은 고민글을 페이지로 조회합니다. 로그인 없이 호출할 수 있습니다.

            - `category`를 보내지 않으면 전체 카테고리입니다.
            - `sort`는 `LATEST`(최신순, 기본값), `VIEW_COUNT`(조회 많은 순), `COMMENT_COUNT`(답변 많은 순)입니다.
            - "지금 가장 핫한 고민"은 `sort=VIEW_COUNT&size=3` 또는 `sort=COMMENT_COUNT&size=3`으로 조회합니다.
            - 조회 수는 비동기로 집계되어 정렬에 바로 반영되지 않을 수 있습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: page, size, category 또는 sort가 올바르지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun listConcerns(
        @ParameterObject @Valid request: ConcernListRequest,
    ): ResponseEntity<SuccessResponse<PageResponse<ConcernSummaryResponse>>>

    @Operation(
        operationId = "getPublicConcern",
        summary = "고민글 상세 조회",
        description = """
            고민글을 조회합니다. 로그인 없이 호출할 수 있고, 토큰을 보내면 `mine`이 채워집니다.

            - 조회할 때마다 조회 수가 1 늘지만 비동기라 이번 응답의 `viewCount`에는 반영되지 않습니다.
            - 삭제된 고민글은 404입니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "CONCERN_NOT_FOUND: 고민글을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getConcern(
        @Parameter(hidden = true) userId: Long?,
        @Positive concernId: Long,
    ): ResponseEntity<SuccessResponse<ConcernDetailResponse>>

    @Operation(
        operationId = "createConcern",
        summary = "고민글 작성",
        description = "로그인한 활성 사용자가 고민글을 작성합니다. 본문은 일반 텍스트입니다.",
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "작성 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 요청 필드가 올바르지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요함",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN: 활성 사용자만 작성 가능",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createConcern(
        @Parameter(hidden = true) userId: Long,
        @Valid request: SaveConcernRequest,
    ): ResponseEntity<SuccessResponse<CreateConcernResponse>>

    @Operation(
        operationId = "replaceMyConcern",
        summary = "내 고민글 수정",
        description = "작성자 본인이 카테고리·제목·본문을 모두 보내 고민글을 바꿉니다.",
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 요청 필드가 올바르지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요함",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "CONCERN_PERMISSION_DENIED, USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "CONCERN_NOT_FOUND: 고민글을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceConcern(
        @Parameter(hidden = true) userId: Long,
        @Positive concernId: Long,
        @Valid request: SaveConcernRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteMyConcern",
        summary = "내 고민글 삭제",
        description = """
            작성자 본인의 고민글을 소프트 삭제합니다. 이미 지운 고민글을 다시 지워도 200입니다.

            - 고민글에 달린 답변·답글은 그대로 남지만 고민글이 보이지 않아 조회할 수 없습니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요함",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "CONCERN_PERMISSION_DENIED: 작성자가 아님",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "CONCERN_NOT_FOUND: 고민글을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteConcern(
        @Parameter(hidden = true) userId: Long,
        @Positive concernId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
