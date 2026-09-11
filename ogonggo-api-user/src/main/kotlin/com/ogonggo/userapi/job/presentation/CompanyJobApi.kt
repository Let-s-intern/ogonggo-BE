package com.ogonggo.userapi.job.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.job.presentation.request.CreateCompanyJobRequest
import com.ogonggo.userapi.job.presentation.request.UpdateCompanyJobRequest
import com.ogonggo.userapi.job.presentation.response.CompanyJobDetailResponse
import com.ogonggo.userapi.job.presentation.response.CompanyJobSummaryResponse
import com.ogonggo.userapi.job.presentation.response.CreateCompanyJobResponse
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
import org.springframework.http.ResponseEntity

private const val COMPANY_FORBIDDEN_DESCRIPTION =
    "COMPANY_ROLE_REQUIRED, USER_SUSPENDED 또는 USER_WITHDRAWN"
private const val JOB_NOT_FOUND_DESCRIPTION =
    "JOB_NOT_FOUND: 채용공고를 찾을 수 없거나 내 공고가 아닙니다."

@Tag(name = "기업회원 채용공고")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface CompanyJobApi {

    @Operation(
        operationId = "createMyJob",
        summary = "채용공고 등록",
        description = """
            등록한 공고는 항상 임시저장 상태로 만들어집니다. 지원자에게 노출하려면 게시를 따로 요청해야 합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "403",
                description = COMPANY_FORBIDDEN_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createJob(
        @Parameter(hidden = true) userId: Long,
        @Valid request: CreateCompanyJobRequest,
    ): ResponseEntity<SuccessResponse<CreateCompanyJobResponse>>

    @Operation(operationId = "listMyJobs",
summary = "내 채용공고 목록 조회", description = "최근에 등록한 공고부터 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "403",
                description = COMPANY_FORBIDDEN_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getJobs(
        @Parameter(hidden = true) userId: Long,
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<CompanyJobSummaryResponse>>>

    @Operation(operationId = "getMyJob", summary = "내 채용공고 상세 조회")
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
        @Parameter(hidden = true) userId: Long,
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<CompanyJobDetailResponse>>

    @Operation(operationId = "replaceMyJob",
summary = "내 채용공고 수정", description = "보낸 값으로 공고 전체를 교체합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = JOB_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateJob(
        @Parameter(hidden = true) userId: Long,
        @Positive jobId: Long,
        @Valid request: UpdateCompanyJobRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "publishMyJob",
        summary = "내 채용공고 게시",
        description = "임시저장한 공고를 지원자에게 노출합니다. 이미 게시된 공고를 다시 게시해도 성공합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "게시 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = JOB_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun publishJob(
        @Parameter(hidden = true) userId: Long,
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "closeMyJob",
        summary = "내 채용공고 마감",
        description = "모집을 마감합니다. 이미 마감한 공고를 다시 마감해도 최초 마감 일시를 유지합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "마감 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = JOB_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun closeJob(
        @Parameter(hidden = true) userId: Long,
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(operationId = "deleteMyJob",
summary = "내 채용공고 삭제", description = "여러 번 삭제해도 최초 삭제 일시를 유지합니다.")
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
        @Parameter(hidden = true) userId: Long,
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
