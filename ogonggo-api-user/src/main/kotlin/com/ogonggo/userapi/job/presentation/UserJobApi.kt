package com.ogonggo.userapi.job.presentation

import com.ogonggo.core.job.domain.JobEmploymentType
import com.ogonggo.core.job.domain.JobExperienceType
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.job.presentation.response.UserJobCalendarItemResponse
import com.ogonggo.userapi.job.presentation.response.UserJobDetailResponse
import com.ogonggo.userapi.job.presentation.response.UserJobSummaryResponse
import com.ogonggo.userapi.job.presentation.response.UserTodayJobSummaryResponse
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

            sort로 정렬을 고릅니다. LATEST는 최신순, VIEW_COUNT는 조회수순이며 조회 수가 같으면 등록 역순입니다.
            LATEST는 크롤러가 수집한 공고를 먼저 보이고, 그 안에서 늦게 등록한 날의 공고를 먼저 보입니다.
            같은 날 등록한 공고끼리는 한 회사 공고가 몰리지 않도록 섞되, 순서는 요청마다 같습니다.

            employmentType, experienceType, jobField(직군), jobRole(직무), region, subRegion으로 목록을 좁힙니다.
            jobRole은 jobRole=IT_BACKEND&jobRole=IT_FRONTEND처럼 여러 번 보내 여러 개를 고를 수 있고, 그중 하나라도 맞는 공고가 걸립니다.
            나머지는 하나씩 고를 수 있고, 보내지 않으면 해당 조건을 적용하지 않습니다.
            jobField(직군), jobRole(직무)은 GET /api/v1/enums의 JobField·JobRole 값을 보냅니다.
            jobField만 보내면 그 직군의 직무 공고도 함께 걸립니다.
            region(시·도), subRegion(시·군·구)은 GET /api/v1/enums의 Region·SubRegion 값을 보냅니다.
            region만 보내면 그 시·도의 시·군·구 공고도 함께 걸립니다.
            필터끼리, 그리고 정렬과 함께 사용할 수 있습니다.

            keyword는 회사명 또는 공고 제목에 포함되는지로 찾으며 대소문자를 가리지 않습니다.
            2자 이상 100자 이하여야 하며, 검색하지 않을 때는 보내지 않습니다.
            검색도 필터·정렬과 함께 사용할 수 있습니다.

            recruitmentStatus는 RECRUITING(모집 중), CLOSED(모집 마감) 중 하나이며 보내지 않으면 둘 다 반환합니다.
            마감 처리됐거나 모집 종료 일시가 지났으면 CLOSED, 그 밖에는 RECRUITING이며 상시 채용은 마감 처리 전까지 RECRUITING입니다.
            종료 일시가 지난 공고는 매시 정각에 CLOSED로 바뀌므로 그 전까지는 RECRUITING으로 걸릴 수 있습니다.
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
        employmentType: JobEmploymentType?,
        experienceType: JobExperienceType?,
        jobField: JobField?,
        jobRoles: List<JobRole>?,
        region: Region?,
        subRegion: SubRegion?,
        @Size(min = 2, max = 100)
        keyword: String?,
        recruitmentStatus: JobRecruitmentStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserJobSummaryResponse>>>

    @Operation(
        operationId = "listPublicPopularJobs",
        summary = "인기 채용공고 조회",
        description = """
            조회 수가 가장 높은 채용공고를 최대 4건 반환합니다. 페이지 정보는 없습니다.

            로그인 없이 조회할 수 있습니다. 액세스 토큰을 보내면 bookmarked에 해당 사용자의 북마크 여부가 담기고,
            보내지 않으면 항상 false입니다.

            employmentType을 보내면 해당 고용 형태의 공고 중에서 고릅니다. 예를 들어 FULL_TIME은 정규직,
            INTERN은 인턴 인기 공고입니다. 보내지 않으면 고용 형태와 관계없이 전체에서 고릅니다.

            게시 중인 공고 중 마감 처리되지 않았고 모집 종료 일시가 지나지 않은 공고만 대상입니다.
            모집 종료 일시가 없는 ALWAYS_OPEN 공고는 포함하며, 한 번도 조회되지 않은 공고는 포함하지 않습니다.
            조회 수 내림차순이며 조회 수가 같으면 최신순입니다.

            조회 수 기록은 비동기이므로 가장 최근 조회가 즉시 반영되지 않을 수 있습니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun getPopularJobs(
        @Parameter(hidden = true)
        userId: Long?,
        employmentType: JobEmploymentType?,
    ): ResponseEntity<SuccessResponse<List<UserJobSummaryResponse>>>

    @Operation(
        operationId = "listPublicTodayJobs",
        summary = "오늘의 채용공고 조회",
        description = """
            운영자가 관리자 콘솔에서 고른 오늘의 공고를 고른 순서대로 반환합니다. 페이지 정보는 없습니다.
            개수는 운영자가 정하며 고른 공고가 없으면 빈 목록입니다.

            로그인 없이 조회할 수 있습니다. 액세스 토큰을 보내면 bookmarked에 해당 사용자의 북마크 여부가 담기고,
            보내지 않으면 항상 false입니다.

            항목은 채용공고 목록 항목에 운영자가 공고마다 적은 추천 문구(recommendationTitle, recommendationDescription)를 더한 것입니다.

            게시 중인 공고만 반환합니다. 고른 뒤에 숨기거나 삭제한 공고는 빠집니다.
            마감 처리됐거나 모집 종료 일시가 지난 공고는 운영자가 뺄 때까지 그대로 나오며,
            closedAt과 recruitmentEndAt으로 마감 여부를 알 수 있습니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun getTodayJobs(
        @Parameter(hidden = true)
        userId: Long?,
    ): ResponseEntity<SuccessResponse<List<UserTodayJobSummaryResponse>>>

    @Operation(
        operationId = "listMySimilarJobs",
        summary = "비슷한 채용공고 조회",
        description = """
            내 정보의 희망 직무(wishJob)와 희망 산업(wishIndustry)에 맞는 채용공고를 최대 4건 반환합니다.
            사용자마다 결과가 다르므로 로그인이 필요합니다.

            희망 값은 쉼표로 나눠 앞뒤 공백을 지운 뒤 비교합니다. 희망 직무는 직무(JobRole) 라벨과 같은 값만
            그 직무로 보고, 희망 산업은 공고의 산업(industry)과 정확히 같은지 비교합니다.
            직무와 산업이 모두 맞는 공고, 직무만 맞는 공고, 산업만 맞는 공고 순으로 채우며
            각 순서 안에서는 조회 수 내림차순이고 조회 수가 같으면 최신순입니다.

            게시 중인 공고 중 마감 처리되지 않았고 모집 종료 일시가 지나지 않은 공고만 대상입니다.
            맞는 공고가 4건보다 적으면 있는 만큼만 반환하고 다른 공고로 채우지 않습니다.
            희망 직무와 산업이 모두 비어 있거나 기업 회원이면 빈 목록입니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getSimilarJobs(
        @Parameter(hidden = true)
        userId: Long,
    ): ResponseEntity<SuccessResponse<List<UserJobSummaryResponse>>>

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

            employmentType, experienceType, jobField(직군), jobRole(직무), region, subRegion, keyword는 채용공고 목록 조회와 같습니다.
            jobRole만 여러 번 보내 여러 개를 고를 수 있고 나머지는 하나씩 고릅니다.
            보내지 않으면 해당 조건을 적용하지 않으며 서로 함께 사용할 수 있습니다.
            keyword는 회사명 또는 공고 제목에 포함되는지로 찾으며 대소문자를 가리지 않고 2자 이상 100자 이하여야 합니다.

            excludeClosed=true면 마감 처리됐거나 모집 종료 일시가 지난 공고를 뺍니다.
            deadlineOnly=true(마감일 기준)면 기간이 겹치는 공고 대신 모집 종료 일시가 from~to 안에 있는 공고만 반환합니다.
            bookmarkedOnly=true면 내가 북마크한 공고만 반환하며, 이때만 로그인이 필요하고 토큰이 없으면 401입니다.
            세 값은 기본이 false이며 다른 필터와 함께 사용할 수 있습니다.

            로그인 없이 조회할 수 있습니다. 액세스 토큰을 보내면 bookmarked에 해당 사용자의 북마크 여부가 담기고,
            보내지 않으면 항상 false입니다.

            응답에 페이지네이션이 없어 조회 기간이 곧 응답 크기가 되므로 from부터 to까지 최대 92일만 허용합니다.
            날짜별 목록의 더보기는 받은 목록을 클라이언트가 나눠 보여 줍니다.
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
                description = "BAD_REQUEST: 시작일이 종료일보다 늦거나 조회 기간이 92일을 넘거나 필터 값이 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: bookmarkedOnly=true인데 로그인하지 않았습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun getJobCalendar(
        @Parameter(hidden = true)
        userId: Long?,
        from: LocalDate,
        to: LocalDate,
        employmentType: JobEmploymentType?,
        experienceType: JobExperienceType?,
        jobField: JobField?,
        jobRoles: List<JobRole>?,
        region: Region?,
        subRegion: SubRegion?,
        @Size(min = 2, max = 100)
        keyword: String?,
        excludeClosed: Boolean,
        bookmarkedOnly: Boolean,
        deadlineOnly: Boolean,
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
