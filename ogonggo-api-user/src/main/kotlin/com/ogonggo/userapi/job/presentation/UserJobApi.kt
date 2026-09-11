package com.ogonggo.userapi.job.presentation

import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
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
import jakarta.validation.constraints.Size
import java.time.LocalDate
import org.springframework.http.ResponseEntity

@Tag(name = "채용공고")
interface UserJobApi {

    @Operation(
        operationId = "listPublicJobs",
        summary = "채용공고 목록 조회",
        description = """
            로그인 없이 조회할 수 있습니다. 액세스 토큰을 보내면 bookmarked에 해당 사용자의 북마크 여부가 담기고,
            보내지 않으면 항상 false입니다.

            sort로 정렬을 고릅니다. LATEST는 최신순, VIEW_COUNT는 조회수순이며 조회 수가 같으면 최신순입니다.

            employmentType과 experienceType으로 목록을 좁힙니다. 각각 하나씩 고를 수 있고,
            보내지 않으면 해당 조건을 적용하지 않습니다. 두 필터와 정렬은 함께 사용할 수 있습니다.

            keyword는 회사명 또는 공고 제목에 포함되는지로 찾으며 대소문자를 가리지 않습니다.
            2자 이상 100자 이하여야 하며, 검색하지 않을 때는 보내지 않습니다.
            검색도 필터·정렬과 함께 사용할 수 있습니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun getJobs(
        @Parameter(hidden = true)
        userId: Long?,
        @Min(1)
        page: Int,
        @Min(1)
        @Max(100)
        size: Int,
        sortType: JobSortType,
        employmentType: EmploymentType?,
        experienceType: ExperienceType?,
        @Size(min = 2, max = 100)
        keyword: String?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserJobSummaryResponse>>>

    @Operation(
        operationId = "createJobSourceUrlClick",
        summary = "채용공고 원문 이동 기록",
        description = """
            사용자가 채용공고 원문으로 이동하는 버튼을 눌렀다는 사실을 기록합니다.

            누가 눌렀는지를 남기는 기록이므로 목록·상세 조회와 달리 로그인이 필요합니다.
            같은 사용자가 같은 공고를 여러 번 눌러도 최초 기록만 남기고 항상 성공합니다.
            이동할 주소는 상세 조회 응답의 sourceUrl을 사용합니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
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
    fun recordSourceUrlClick(
        @Parameter(hidden = true)
        userId: Long,
        @Positive
        jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "listPublicJobCalendar",
        summary = "채용공고 달력 조회",
        description = """
            모집 기간이 조회 범위와 **겹치는** 게시 공고를 반환합니다.
            그 기간에 시작하는 공고도, 끝나는 공고도 아니라 그 기간에 모집이 진행 중인 공고입니다.

            조회 범위는 from 당일 00:00부터 to 당일 끝까지이며, 하루라도 겹치면 포함됩니다.
            예를 들어 from=2026-09-05, to=2026-09-07로 조회하면
            모집 기간이 2026-09-06~2026-09-08인 공고도, 2026-08-25~2026-09-06인 공고도 함께 나옵니다.
            반대로 2026-09-08에 시작하거나 2026-09-04에 끝난 공고는 나오지 않습니다.

            모집 기간이 없는 ALWAYS_OPEN 공고는 제외합니다.
            시작·종료 일시가 모두 있는 공고만 대상이며 종료 일시, 식별자 오름차순으로 정렬합니다.

            응답에 페이지네이션이 없어 조회 기간이 곧 응답 크기가 되므로 from부터 to까지 최대 92일만 허용합니다.
        """,
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
    fun getJobCalendar(
        from: LocalDate,
        to: LocalDate,
    ): ResponseEntity<SuccessResponse<List<UserJobCalendarItemResponse>>>

    @Operation(
        operationId = "getPublicJob",
        summary = "채용공고 상세 조회",
        description = """
            로그인 없이 조회할 수 있습니다. 액세스 토큰을 보내면 bookmarked에 해당 사용자의 북마크 여부가 담기고,
            보내지 않으면 항상 false입니다.
        """,
    )
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
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun getJob(
        @Parameter(hidden = true)
        userId: Long?,
        @Positive
        jobId: Long,
    ): ResponseEntity<SuccessResponse<UserJobDetailResponse>>
}
