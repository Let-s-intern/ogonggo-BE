package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.config.ADMIN_INTERNAL_API_KEY_SCHEME
import com.ogonggo.adminapi.job.presentation.request.CrawlerJobRegistrationRequest
import com.ogonggo.adminapi.job.presentation.request.CrawlerJobReplaceRequest
import com.ogonggo.adminapi.job.presentation.response.CrawlerJobLookupResponse
import com.ogonggo.adminapi.job.presentation.response.CrawlerJobRegistrationResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

private const val BAD_REQUEST_DESCRIPTION = "BAD_REQUEST: 요청 값이 올바르지 않거나 판단 값(고용 형태·경력 유형·요구 학력·모집 기간 유형)이 없습니다."
private const val UNAUTHORIZED_DESCRIPTION = "UNAUTHORIZED: 내부 API 키가 없거나 올바르지 않습니다."
private const val CRAWLED_JOB_NOT_FOUND_DESCRIPTION = "JOB_NOT_FOUND: 수집 공고가 없습니다. 기업회원 공고도 찾지 않습니다."

@Tag(name = "크롤러 채용공고")
@SecurityRequirement(name = ADMIN_INTERNAL_API_KEY_SCHEME)
interface CrawlerJobApi {

    @Operation(
        operationId = "createCrawlerJob",
        summary = "크롤러 채용공고 등록",
        description = """
            크롤러가 수집한 채용공고를 초안·검수 대기 상태로 등록합니다. 운영자가 검수에서 승인하면 게시됩니다.

            고용 형태, 경력 유형, 요구 학력, 모집 기간 유형은 크롤러가 판단해 반드시 보냅니다. 서버는 빈 값을 다른 값으로 채우지 않습니다.
            상시 채용에는 모집 종료 일시를 보낼 수 없습니다.
            이미 같은 원문 URL로 등록된 미삭제 공고가 있으면 409로 거절합니다. 이때는 원문 URL로 식별자를 찾아 교체합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "JOB_ALREADY_EXISTS: 이미 등록된 원문 URL입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun registerJob(
        @Valid request: CrawlerJobRegistrationRequest,
    ): ResponseEntity<SuccessResponse<CrawlerJobRegistrationResponse>>

    @Operation(
        operationId = "getCrawlerJob",
        summary = "원문 URL로 크롤러 채용공고 식별자 조회",
        description = """
            원문 URL로 미삭제 수집 공고의 식별자를 찾습니다.
            크롤러가 등록 응답의 식별자를 잃은 채 같은 공고를 다시 등록해 409를 받았을 때 씁니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CRAWLED_JOB_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getJob(
        @NotBlank @Size(max = 2048) sourceUrl: String,
    ): ResponseEntity<SuccessResponse<CrawlerJobLookupResponse>>

    @Operation(
        operationId = "replaceCrawlerJob",
        summary = "크롤러 채용공고 교체",
        description = """
            다시 수집·분류한 값으로 수집 공고 전체를 바꿉니다. 태그는 바꾸지 않습니다.
            운영자가 관리자 콘솔에서 고친 내용도 이 값으로 덮어씁니다.

            값이 실제로 바뀌면 승인되었거나 반려된 공고를 검수 대기로 되돌리고, 게시 중이었다면 숨깁니다.
            같은 값을 다시 보내면 검수 상태와 게시 상태를 바꾸지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "교체 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CRAWLED_JOB_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "JOB_ALREADY_EXISTS(다른 공고가 쓰는 원문 URL로 바꾸려 함) 또는 JOB_ARCHIVED",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceJob(
        @Positive jobId: Long,
        @Valid request: CrawlerJobReplaceRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteCrawlerJob",
        summary = "크롤러 채용공고 삭제",
        description = """
            직무별로 나뉘어 새 공고로 등록된 원래 공고처럼 더는 쓰지 않는 수집 공고를 소프트 삭제합니다.
            이미 삭제한 공고를 다시 삭제해도 성공하며 최초 삭제 일시를 유지합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CRAWLED_JOB_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteJob(
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
