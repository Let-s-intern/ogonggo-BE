package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostCommentRequest
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostCommentResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "모집글 댓글")
@RequestMapping("/api/v1/recruitment-posts/{postId}/comments")
interface RecruitmentPostCommentApi {

    @Operation(
        operationId = "createRecruitmentPostComment",
        summary = "사이드 프로젝트·스터디 모집글 댓글 작성",
        description = "공개된 모집글에 일반 댓글 또는 1단계 대댓글을 작성합니다.",
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
            ApiResponse(
                responseCode = "409",
                description = "RECRUITMENT_POST_COMMENT_PARENT_DELETED: 삭제된 댓글에는 답글을 작성할 수 없음",
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
}
