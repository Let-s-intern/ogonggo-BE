package com.ogonggo.userapi.concern.presentation

import com.ogonggo.userapi.concern.presentation.request.CreateConcernCommentRequest
import com.ogonggo.userapi.concern.presentation.response.ConcernCommentResponse
import com.ogonggo.userapi.concern.presentation.response.ConcernCommentRootResponse
import com.ogonggo.userapi.concern.presentation.response.CreateConcernCommentResponse
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
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

@Tag(name = "취준고민 답변")
interface ConcernCommentApi {

    @Operation(
        operationId = "listPublicConcernComments",
        summary = "고민글 답변 목록 조회",
        description = """
            답변(부모 댓글)을 페이지로 조회하고 답변마다 앞쪽 답글 5개를 함께 줍니다. 로그인 없이 호출할 수 있고,
            토큰을 보내면 `mine`, `helpfulVoted`가 채워집니다.

            - 운영자 답변(`official=true`)을 먼저 두고, 그 안에서 먼저 단 답변부터 줍니다.
            - 삭제된 답변은 남은 답글이 있을 때만 `"삭제된 댓글입니다"`로 남고, 없으면 목록에서 빠집니다.
            - 답글 수는 `replies.pageInfo.totalElements`입니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: concernId, page 또는 size가 올바르지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "CONCERN_NOT_FOUND: 고민글을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun listComments(
        @Parameter(hidden = true) userId: Long?,
        @Positive concernId: Long,
        @Min(1) page: Int,
        @Min(1) @Max(30) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<ConcernCommentRootResponse>>>

    @Operation(
        operationId = "listPublicConcernCommentReplies",
        summary = "고민글 답글 더보기",
        description = """
            답변 하나의 답글을 먼저 단 순서로 조회합니다. 삭제된 답변의 남은 답글도 조회할 수 있습니다.

            - `commentId`는 답변(부모 댓글)이어야 합니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 요청 파라미터가 올바르지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "CONCERN_NOT_FOUND 또는 CONCERN_COMMENT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun listReplies(
        @Parameter(hidden = true) userId: Long?,
        @Positive concernId: Long,
        @Positive commentId: Long,
        @Min(1) page: Int,
        @Min(1) @Max(30) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<ConcernCommentResponse>>>

    @Operation(
        operationId = "createConcernComment",
        summary = "고민글 답변·답글 작성",
        description = """
            `parentId`가 없으면 답변을, 있으면 그 답변에 답글을 작성합니다. 답글에는 다시 답글을 달 수 없습니다.

            - 관리자 계정으로 작성하면 운영자 답변(`official=true`)이 됩니다.
            - 답변을 작성하면 고민글의 `commentCount`가 1 늘고, 답글은 세지 않습니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "작성 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST 또는 CONCERN_COMMENT_NESTING_NOT_ALLOWED: 답글에 답글을 달 수 없음",
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
            ApiResponse(
                responseCode = "404",
                description = "CONCERN_NOT_FOUND 또는 CONCERN_COMMENT_PARENT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createComment(
        @Parameter(hidden = true) userId: Long,
        @Positive concernId: Long,
        @Valid request: CreateConcernCommentRequest,
    ): ResponseEntity<SuccessResponse<CreateConcernCommentResponse>>

    @Operation(
        operationId = "deleteMyConcernComment",
        summary = "내 답변·답글 삭제",
        description = """
            작성자 본인의 답변이나 답글을 소프트 삭제합니다.

            - 답변을 지우면 고민글의 `commentCount`가 1 줄어듭니다. 남은 답글은 그대로 보입니다.
            - 이미 지운 댓글은 404입니다.
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
                description = "CONCERN_COMMENT_PERMISSION_DENIED: 작성자가 아님",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "CONCERN_NOT_FOUND 또는 CONCERN_COMMENT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteComment(
        @Parameter(hidden = true) userId: Long,
        @Positive concernId: Long,
        @Positive commentId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "replaceMyConcernCommentHelpfulVote",
        summary = "도움돼요 누르기",
        description = "답변·답글에 도움돼요를 누릅니다. 이미 누른 상태에서 다시 보내도 200이며 한 번으로 셉니다.",
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요함",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "CONCERN_NOT_FOUND 또는 CONCERN_COMMENT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun voteHelpful(
        @Parameter(hidden = true) userId: Long,
        @Positive concernId: Long,
        @Positive commentId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteMyConcernCommentHelpfulVote",
        summary = "도움돼요 취소",
        description = "누른 도움돼요를 취소합니다. 누르지 않은 상태에서 보내도 200입니다.",
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요함",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "CONCERN_NOT_FOUND 또는 CONCERN_COMMENT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun cancelHelpful(
        @Parameter(hidden = true) userId: Long,
        @Positive concernId: Long,
        @Positive commentId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
