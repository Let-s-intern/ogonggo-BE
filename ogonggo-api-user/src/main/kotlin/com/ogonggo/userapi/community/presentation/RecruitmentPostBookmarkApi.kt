package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.presentation.response.RecruitmentPostSummaryResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.CursorPageResponse
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
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

@Tag(name = "사이드·스터디 모집글 북마크")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface RecruitmentPostBookmarkApi {

    @Operation(
        operationId = "listMyRecruitmentPostBookmarks",
        summary = "사이드·스터디 모집글 북마크 목록 조회",
        description = "북마크를 마지막으로 활성화한 순서의 커서 페이지를 조회합니다.",
    )
    fun getBookmarks(
        @Parameter(hidden = true) userId: Long,
        cursor: String?,
        @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<CursorPageResponse<RecruitmentPostSummaryResponse>>>

    @Operation(operationId = "createRecruitmentPostBookmark", summary = "사이드·스터디 모집글 북마크 등록")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 모집글을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS: 이미 북마크한 모집글입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun addBookmark(
        @Parameter(hidden = true) userId: Long,
        @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(operationId = "deleteRecruitmentPostBookmark", summary = "사이드·스터디 모집글 북마크 해제")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "해제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 모집글을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteBookmark(
        @Parameter(hidden = true) userId: Long,
        @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
