package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.config.ADMIN_INTERNAL_API_KEY_SCHEME
import com.ogonggo.adminapi.job.presentation.request.AiJobRegistrationRequest
import com.ogonggo.adminapi.job.presentation.request.AiJobReplaceRequest
import com.ogonggo.adminapi.job.presentation.request.AiJobSourceHtmlRequest
import com.ogonggo.adminapi.job.presentation.response.AiJobLookupResponse
import com.ogonggo.adminapi.job.presentation.response.AiJobRegistrationResponse
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

@Tag(name = "AI 서버 채용공고")
@SecurityRequirement(name = ADMIN_INTERNAL_API_KEY_SCHEME)
interface AiJobApi {

    @Operation(
        operationId = "createAiJob",
        summary = "AI 서버 채용공고 등록",
        description = """
            AI 서버가 수집한 채용공고를 게시 상태로 등록합니다. 검수는 기업회원이 올린 공고만 거칩니다.

            고용 형태, 경력 유형, 요구 학력, 모집 기간 유형은 AI 서버가 판단해 반드시 보냅니다. 서버는 빈 값을 다른 값으로 채우지 않습니다.
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
        @Valid request: AiJobRegistrationRequest,
    ): ResponseEntity<SuccessResponse<AiJobRegistrationResponse>>

    @Operation(
        operationId = "getAiJob",
        summary = "원문 URL로 AI 서버 채용공고 식별자 조회",
        description = """
            원문 URL로 미삭제 수집 공고의 식별자를 찾습니다.
            AI 서버가 등록 응답의 식별자를 잃은 채 같은 공고를 다시 등록해 409를 받았을 때 씁니다.
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
    ): ResponseEntity<SuccessResponse<AiJobLookupResponse>>

    @Operation(
        operationId = "replaceAiJob",
        summary = "AI 서버 채용공고 교체",
        description = """
            다시 수집·분류한 값으로 수집 공고 전체를 바꿉니다. 태그는 바꾸지 않습니다.
            운영자가 관리자 콘솔에서 고친 내용도 이 값으로 덮어씁니다. 게시 상태는 바꾸지 않습니다.
            원문(sourceHtml)을 생략하면 저장된 원문을 그대로 둡니다.
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
                description = "JOB_ALREADY_EXISTS: 다른 공고가 쓰는 원문 URL로 바꾸려 함",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceJob(
        @Positive jobId: Long,
        @Valid request: AiJobReplaceRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "changeAiJobSourceHtml",
        summary = "AI 서버 채용공고 원문 변경",
        description = """
            수집 공고의 원문 HTML만 바꿉니다. 원문을 저장하기 전에 보낸 공고를 채울 때 씁니다.
            안전한 태그만 남겨 저장하며, 남은 글도 이미지도 없으면 저장된 원문을 그대로 둡니다.
            운영자가 관리자 콘솔에서 고친 다른 칸은 바꾸지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 원문이 비어 있습니다.",
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
        ],
    )
    fun changeSourceHtml(
        @Positive jobId: Long,
        @Valid request: AiJobSourceHtmlRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "hideAiJob",
        summary = "AI 서버 채용공고 숨김",
        description = """
            수집 공고를 숨김 상태로 바꿔 사용자 화면에서 내립니다. 지우지는 않아 관리자 콘솔에서 다시 노출할 수 있습니다.
            AI 분석을 끝내 하지 못한 공고처럼 보여 줄 수 없는 공고에 씁니다. 크롤링 공고와 고용24 공고를 숨깁니다.
            이미 숨긴 공고를 다시 숨겨도 성공합니다. 삭제한 공고와 기업회원 공고는 찾지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "숨김 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "JOB_NOT_FOUND: 크롤링·고용24 공고가 없습니다. 기업회원 공고도 찾지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun hideJob(
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "closeAiJob",
        summary = "AI 서버 채용공고 마감",
        description = """
            모집을 마감합니다. 마감일이 없는 상시 채용 공고가 원래 사이트 목록에서 사라졌을 때 씁니다.
            게시 상태는 바꾸지 않아 사용자 화면에 모집 마감으로 보입니다. AI 서버가 등록한 크롤링 공고만 마감합니다.
            이미 마감한 공고를 다시 마감해도 처음 마감 일시를 그대로 두고 성공합니다. 삭제한 공고, 고용24 공고, 기업회원 공고는 찾지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "마감 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "JOB_NOT_FOUND: 크롤링 공고가 없습니다. 고용24 공고와 기업회원 공고도 찾지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun closeJob(
        @Positive jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteAiJob",
        summary = "AI 서버 채용공고 삭제",
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
