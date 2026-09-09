package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostRequest
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostResponse
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
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "사이드·스터디 모집")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
@RequestMapping("/api/v1/recruitment-posts")
interface RecruitmentPostApi {

    @Operation(summary = "사이드 프로젝트·스터디 모집글 생성")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "요청 필드 검증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping
    fun createRecruitmentPost(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: CreateRecruitmentPostRequest,
    ): ResponseEntity<SuccessResponse<CreateRecruitmentPostResponse>>
}
