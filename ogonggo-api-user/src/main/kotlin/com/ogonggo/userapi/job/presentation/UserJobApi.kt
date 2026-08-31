package com.ogonggo.userapi.job.presentation

import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.job.presentation.response.UserJobCalendarItemResponse
import com.ogonggo.userapi.job.presentation.response.UserJobDetailResponse
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
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "채용공고")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
@RequestMapping("/api/v1/jobs")
interface UserJobApi {

    @Operation(
        summary = "채용공고 목록 조회",
        description = "sort로 정렬을 고릅니다. LATEST는 최신순, VIEW_COUNT는 조회수순이며 조회 수가 같으면 최신순입니다.",
    )
    @GetMapping
    fun getJobs(
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
        @RequestParam(name = "sort", defaultValue = "LATEST")
        sortType: JobSortType,
    ): ResponseEntity<SuccessResponse<PageResponse<UserJobSummaryResponse>>>

    @Operation(
        summary = "채용공고 원문 이동 기록",
        description = """
            사용자가 채용공고 원문으로 이동하는 버튼을 눌렀다는 사실을 기록합니다.

            같은 사용자가 같은 공고를 여러 번 눌러도 최초 기록만 남기고 항상 성공합니다.
            이동할 주소는 상세 조회 응답의 sourceUrl을 사용합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "기록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "JOB_NOT_FOUND: 게시된 채용공고를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/{jobId}/source-url-clicks")
    fun recordSourceUrlClick(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: Long,
        @PathVariable("jobId")
        @Positive
        jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        summary = "채용공고 달력 조회",
        description = "모집 기간이 조회 범위와 겹치는 게시 공고를 반환합니다. 조회 기간은 최대 92일입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                useReturnTypeSchema = true,
            ),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 시작일이 종료일보다 늦거나 조회 기간이 92일을 넘습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/calendar")
    fun getJobCalendar(
        @RequestParam("from")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        from: LocalDate,
        @RequestParam("to")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        to: LocalDate,
    ): ResponseEntity<SuccessResponse<List<UserJobCalendarItemResponse>>>

    @Operation(summary = "채용공고 상세 조회")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                useReturnTypeSchema = true,
            ),
            ApiResponse(
                responseCode = "404",
                description = "JOB_NOT_FOUND: 일자리 공고를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/{jobId}")
    fun getJob(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: Long,
        @PathVariable("jobId")
        @Positive
        jobId: Long,
    ): ResponseEntity<SuccessResponse<UserJobDetailResponse>>
}
