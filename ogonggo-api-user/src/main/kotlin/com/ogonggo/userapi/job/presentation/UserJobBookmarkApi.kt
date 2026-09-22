package com.ogonggo.userapi.job.presentation

import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobApplicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.job.presentation.request.UpdateJobApplicationStatusRequest
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
import jakarta.validation.Valid
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
            북마크한 공고 중 게시 중인 공고만 반환합니다.

            채용공고 목록과 같은 필터를 사용할 수 있습니다.
            employmentType, experienceType, jobField(직군), jobRole(직무)로 목록을 좁히며 각각 하나씩 고를 수 있고,
            보내지 않으면 해당 조건을 적용하지 않습니다. jobField와 jobRole은 공고의 값과 정확히 같은지로 거릅니다.

            keyword는 회사명 또는 공고 제목에 포함되는지로 찾으며 대소문자를 가리지 않습니다.
            2자 이상 100자 이하여야 하며, 검색하지 않을 때는 보내지 않습니다.

            applicationStatus는 지원·신청 관리 단계로 SCRAPPED(스크랩), PREPARING(지원 준비 중), APPLIED(지원 완료),
            INTERVIEWING(면접), PASSED(합격), FAILED(불합격) 중 하나입니다.
            보내면 그 단계의 북마크만 반환하고, 보내지 않으면 모든 단계를 반환합니다.

            recruitmentStatus는 RECRUITING(모집 중), CLOSED(모집 마감) 중 하나입니다.
            마감 처리됐거나 모집 종료 일시가 지났으면 CLOSED, 그 밖에는 RECRUITING이며 상시 채용은 마감 처리 전까지 RECRUITING입니다.

            sort로 정렬을 고릅니다. 지금은 RECENTLY_SAVED(최근 저장순)만 있으며 보내지 않으면 RECENTLY_SAVED입니다.
            북마크를 등록·재등록하거나 지원 단계를 옮긴 시각이 최근인 순서입니다.
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
        sortType: BookmarkSortType,
        employmentType: EmploymentType?,
        experienceType: ExperienceType?,
        @Size(max = 100)
        jobField: String?,
        @Size(max = 100)
        jobRole: String?,
        @Size(min = 2, max = 100)
        keyword: String?,
        applicationStatus: JobApplicationStatus?,
        recruitmentStatus: JobRecruitmentStatus?,
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

    @Operation(
        operationId = "replaceMyJobBookmarkApplicationStatus",
        summary = "채용공고 북마크의 지원·신청 관리 단계 변경",
        description = """
            북마크를 applicationStatus 단계로 옮깁니다. 단계는 스크랩, 지원 준비 중, 지원 완료, 면접, 합격, 불합격입니다.
            단계 사이에 선후 관계가 없어 어느 단계에서든 다른 어느 단계로든 옮길 수 있습니다.
            이미 그 단계면 아무것도 바꾸지 않고 200으로 응답합니다.
            옮긴 북마크는 해당 단계 목록의 맨 앞에 옵니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "이동 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: applicationStatus가 없거나 정의되지 않은 단계입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "JOB_BOOKMARK_NOT_FOUND: 북마크하지 않은 일자리 공고입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateApplicationStatus(
        @Parameter(hidden = true)
        userId: Long,
        @Positive
        jobId: Long,
        @Valid
        request: UpdateJobApplicationStatusRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
