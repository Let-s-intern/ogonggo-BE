package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.presentation.response.RecruitmentPostSummaryResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.PageResponse
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
        description = """
            북마크를 마지막으로 활성화한 순서의 페이지를 조회합니다.

            ### 추가사항

            - 현재 활성화된 북마크 중 공개 글만 반환합니다.
            - 응답 아이템의 `bookmarked`는 항상 `true`입니다.
        """,
    )
    fun getBookmarks(
        @Parameter(hidden = true) userId: Long,
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostSummaryResponse>>>

    @Operation(
        operationId = "createRecruitmentPostBookmark",
        summary = "사이드·스터디 모집글 북마크 등록",
        description = """
            사이드 프로젝트·스터디 모집글을 북마크합니다.

            ### 추가사항

            - `CLOSED` 상태의 공개 모집글도 북마크할 수 있습니다.
            - 삭제된 모집글은 북마크할 수 없습니다.
            - 기존 비활성 북마크가 있으면 재활성화됩니다.
        """,
    )
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

    @Operation(
        operationId = "deleteRecruitmentPostBookmark",
        summary = "사이드·스터디 모집글 북마크 해제",
        description = """
            사이드 프로젝트·스터디 모집글의 북마크를 해제합니다.

            ### 추가사항

            - 삭제된 모집글도 북마크 해제가 가능합니다.
            - 북마크가 없는 상태에서 호출해도 멱등적으로 처리됩니다.
        """,
    )
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
