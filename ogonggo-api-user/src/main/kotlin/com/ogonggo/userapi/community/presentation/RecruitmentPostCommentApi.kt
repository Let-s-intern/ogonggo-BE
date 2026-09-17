package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostCommentRequest
import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostCommentReportRequest
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostCommentResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostCommentResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostCommentRootResponse
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.security.core.annotation.AuthenticationPrincipal

@Tag(name = "모집글 댓글")
@RequestMapping("/api/v1/recruitment-posts/{postId}/comments")
interface RecruitmentPostCommentApi {

    @Operation(
        operationId = "getRecruitmentPostComments",
        summary = "사이드 프로젝트·스터디 모집글 부모 댓글 조회",
        description = """
            부모 댓글을 최신순 페이지로 조회하고 대댓글 미리보기 5건을 함께 반환합니다.

            ### 추가사항

            - 부모 댓글은 최신순으로 반환됩니다.
            - 대댓글은 최대 5개까지 미리보기로 반환됩니다.
            - `CLOSED` 상태의 공개 모집글도 댓글 조회가 가능합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "댓글 조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: postId, page 또는 size가 올바르지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 모집글을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping
    fun getComments(
        @Parameter(hidden = true)
        @AuthenticationPrincipal userId: Long?,
        @PathVariable("postId") @Positive postId: Long,
        @RequestParam(defaultValue = "1") @Min(1) page: Int,
        @RequestParam(defaultValue = "10") @Min(1) @Max(30) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostCommentRootResponse>>>

    @Operation(
        operationId = "getRecruitmentPostCommentReplies",
        summary = "사이드 프로젝트·스터디 모집글 대댓글 더보기",
        description = """
            특정 부모 댓글의 대댓글을 오래된 순서의 페이지로 조회합니다.

            ### 추가사항

            - `commentId`는 부모 댓글이어야 합니다.
            - 대댓글에 다시 답글을 작성할 수 없습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "대댓글 조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 요청 파라미터가 올바르지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_COMMENT_NOT_FOUND: 부모 댓글을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/{commentId}/replies")
    fun getReplies(
        @Parameter(hidden = true)
        @AuthenticationPrincipal userId: Long?,
        @PathVariable("postId") @Positive postId: Long,
        @PathVariable("commentId") @Positive commentId: Long,
        @RequestParam(defaultValue = "1") @Min(1) page: Int,
        @RequestParam(defaultValue = "5") @Min(1) @Max(30) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostCommentResponse>>>

    @Operation(
        operationId = "deleteRecruitmentPostComment",
        summary = "사이드 프로젝트·스터디 모집글 댓글 삭제",
        description = """
            댓글 작성자 본인의 댓글을 소프트 삭제합니다. 부모 댓글 삭제 시 활성 대댓글도 함께 소프트 삭제됩니다.

            ### 추가사항

            - 댓글 작성자 본인만 삭제할 수 있습니다.
            - 삭제된 댓글의 내용은 데이터베이스에 보존되며 일반 댓글 조회에서는 제외됩니다.
            - 실제로 활성 상태에서 삭제된 댓글 수만큼 모집글의 `commentCount`가 감소합니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "댓글 삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요함",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "RECRUITMENT_POST_COMMENT_PERMISSION_DENIED: 댓글 삭제 권한이 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND 또는 RECRUITMENT_POST_COMMENT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: Long,
        @PathVariable("postId") @Positive postId: Long,
        @PathVariable("commentId") @Positive commentId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "createRecruitmentPostComment",
        summary = "사이드 프로젝트·스터디 모집글 댓글 작성",
        description = """
            공개된 모집글에 일반 댓글 또는 1단계 대댓글을 작성합니다.

            ### 추가사항

            - 댓글은 부모 댓글과 1단계 대댓글까지만 지원합니다.
            - 댓글 작성 성공 시 모집글의 `commentCount`가 증가합니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "댓글 작성 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 요청 필드 또는 대댓글 규칙이 올바르지 않음",
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
                description = "RECRUITMENT_POST_NOT_FOUND 또는 RECRUITMENT_POST_COMMENT_PARENT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping
    fun createComment(
        @Parameter(hidden = true)
        userId: Long,
        @PathVariable("postId") @Positive postId: Long,
        @RequestBody @Valid request: CreateRecruitmentPostCommentRequest,
    ): ResponseEntity<SuccessResponse<CreateRecruitmentPostCommentResponse>>

    @Operation(
        operationId = "reportRecruitmentPostComment",
        summary = "사이드 프로젝트·스터디 모집글 댓글 신고",
        description = """
            로그인한 활성 사용자가 댓글을 신고합니다. 신고 사유는 생략할 수 있고, 동일 댓글을 중복 신고할 수 있습니다.

            ### 추가사항

            - 신고 사유는 생략할 수 있습니다.
            - 동일 댓글에 대한 중복 신고 제한은 없습니다.
            - 성공 시 별도 응답 데이터는 없습니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "신고 접수 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 신고 사유가 500자를 초과함",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요함",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN: 활성 사용자만 신고 가능",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND 또는 RECRUITMENT_POST_COMMENT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/{commentId}/reports")
    fun reportComment(
        @Parameter(hidden = true)
        @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") @Positive postId: Long,
        @PathVariable("commentId") @Positive commentId: Long,
        @RequestBody @Valid request: CreateRecruitmentPostCommentReportRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
