package com.ogonggo.userapi.job.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.job.presentation.response.UserJobSummaryResponse
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
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "채용공고 북마크")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
@RequestMapping("/api/v1/job-bookmarks")
interface UserJobBookmarkApi {

    @Operation(summary = "채용공고 북마크 목록 조회")
    @GetMapping
    fun getBookmarks(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: Long,
        @RequestParam(name = "page", defaultValue = "1")
        @Min(1)
        page: Int,
        @RequestParam(name = "size", defaultValue = "10")
        @Min(1)
        @Max(100)
        size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<UserJobSummaryResponse>>>

    @Operation(summary = "채용공고 북마크 등록")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "JOB_NOT_FOUND: 일자리 공고를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "JOB_BOOKMARK_ALREADY_EXISTS: 이미 북마크한 일자리 공고입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/{jobId}")
    fun addBookmark(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: Long,
        @PathVariable("jobId")
        @Positive
        jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(summary = "채용공고 북마크 해제")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "해제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "JOB_NOT_FOUND: 일자리 공고를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @DeleteMapping("/{jobId}")
    fun deleteBookmark(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: Long,
        @PathVariable("jobId")
        @Positive
        jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
