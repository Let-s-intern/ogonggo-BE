package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.content.business.AdminContentSortType
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.job.presentation.request.UpdateAdminJobRequest
import com.ogonggo.adminapi.job.presentation.response.AdminJobDetailResponse
import com.ogonggo.adminapi.job.presentation.response.AdminJobSummaryResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import io.swagger.v3.oas.annotations.Operation
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

private const val ADMIN_AUTH_DESCRIPTION = "UNAUTHORIZED: 액세스 토큰이 없거나 올바르지 않습니다."
private const val ADMIN_FORBIDDEN_DESCRIPTION = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다."
private const val JOB_NOT_FOUND_DESCRIPTION = "JOB_NOT_FOUND: 채용공고를 찾을 수 없습니다."

@Tag(name = "관리자 채용공고")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminJobApi {

    @Operation(
        operationId = "listJobs",
        summary = "채용공고 목록 조회",
        description = """
            게시 상태와 무관하게 삭제되지 않은 채용공고를 반환합니다. 본문 칸은 싣지 않습니다.

            필터는 모두 AND로 묶이며 값을 보내지 않거나 빈 값을 보내면 그 조건을 적용하지 않습니다.
            keyword는 제목과 회사명에서 대소문자를 가리지 않고 부분 일치로 찾습니다.
            visibility는 게시 중이면 VISIBLE, 초안·숨김·보관이면 HIDDEN입니다.
            source는 등록한 기업회원이 있으면 COMPANY, 없으면 CRAWLER입니다.
            recruitmentStatus는 마감 처리됐거나 모집 종료 일시가 지났으면 CLOSED, 그 밖에는 RECRUITING입니다.

            정렬 기본값은 REGISTERED_AT(등록일 역순)이며, VIEW_COUNT는 조회 수가 같으면 등록일 역순입니다.
            마지막 페이지를 넘는 page는 빈 items를 반환합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 페이지 범위나 필터 값이 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = ADMIN_AUTH_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = ADMIN_FORBIDDEN_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getJobs(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        sortType: AdminContentSortType,
        @Size(max = 100) keyword: String?,
        visibility: AdminContentVisibility?,
        source: ContentSource?,
        reviewStatus: ReviewStatus?,
        recruitmentStatus: JobRecruitmentStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminJobSummaryResponse>>>

    @Operation(operationId = "getJob", summary = "채용공고 상세 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = JOB_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getJob(
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<AdminJobDetailResponse>>

    @Operation(
        operationId = "updateJob",
        summary = "채용공고 운영 값·내용 수정",
        description = """
            보낸 값만 바꾸고 수정된 공고 전체를 반환합니다.

            reviewStatus는 APPROVED나 PENDING만 보낼 수 있습니다. 반려는 검수 화면(PATCH /review-queue)에서 사유와 함께 처리합니다.
            승인하면 곧바로 노출되고, 검수 대기로 되돌리면 노출이 꺼지며 반려 기록이 지워집니다.
            검수 상태를 먼저 반영한 뒤 visibility를 반영합니다.
            기업회원 공고는 승인 전에 VISIBLE로 바꿀 수 없습니다. 크롤링 수집분은 검수 상태를 바꿀 수 없습니다.

            fields는 companyAndTeamIntroduction, responsibilities, qualifications, preferredQualifications,
            compensation, benefits, hiringProcess만 반영하고 나머지 키는 버립니다. 빈 문자열은 그 칸을 비웁니다.
            등록 경로(source)는 바꿀 수 없어 보내도 무시합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 제목이 비었거나 reviewStatus에 REJECTED를 보냈습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = JOB_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "REVIEW_NOT_APPROVED, CONTENT_NOT_REVIEWABLE 또는 JOB_ARCHIVED",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateJob(
        @Positive jobId: Long,
        @Valid request: UpdateAdminJobRequest,
    ): ResponseEntity<SuccessResponse<AdminJobDetailResponse>>

    @Operation(
        operationId = "deleteJob",
        summary = "채용공고 삭제",
        description = """
            소프트 삭제합니다. 이미 삭제한 공고를 다시 삭제해도 성공하며 최초 삭제 일시를 유지합니다.
            반려 기록은 지우지 않고 반려 보관 목록에 contentExists=false로 남깁니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = JOB_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteJob(
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
