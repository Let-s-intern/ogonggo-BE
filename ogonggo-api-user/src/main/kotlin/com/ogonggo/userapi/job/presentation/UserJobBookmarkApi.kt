package com.ogonggo.userapi.job.presentation

import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
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
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

@Tag(name = "채용공고 북마크")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface UserJobBookmarkApi {

    @Operation(
        operationId = "listMyJobBookmarks",
        summary = "채용공고 북마크 목록 조회",
        description = """
            북마크한 공고 중 게시 중인 공고만 최근 북마크 순으로 반환합니다.

            채용공고 목록과 같은 필터를 사용할 수 있습니다.
            employmentType, experienceType, jobField(직군), jobRole(직무)로 목록을 좁히며 각각 하나씩 고를 수 있고,
            보내지 않으면 해당 조건을 적용하지 않습니다. jobField와 jobRole은 공고의 값과 정확히 같은지로 거릅니다.

            keyword는 회사명 또는 공고 제목에 포함되는지로 찾으며 대소문자를 가리지 않습니다.
            2자 이상 100자 이하여야 하며, 검색하지 않을 때는 보내지 않습니다.
        """,
    )
    fun getBookmarks(
        @Parameter(hidden = true)
        userId: Long,
        @Min(1)
        page: Int,
        @Min(1)
        @Max(100)
        size: Int,
        employmentType: EmploymentType?,
        experienceType: ExperienceType?,
        @Size(max = 100)
        jobField: String?,
        @Size(max = 100)
        jobRole: String?,
        @Size(min = 2, max = 100)
        keyword: String?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserJobSummaryResponse>>>

    @Operation(operationId = "createJobBookmark", summary = "채용공고 북마크 등록")
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
    fun addBookmark(
        @Parameter(hidden = true)
        userId: Long,
        @Positive
        jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(operationId = "deleteJobBookmark", summary = "채용공고 북마크 해제")
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
    fun deleteBookmark(
        @Parameter(hidden = true)
        userId: Long,
        @Positive
        jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
