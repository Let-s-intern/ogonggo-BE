package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.config.ADMIN_INTERNAL_API_KEY_SCHEME
import com.ogonggo.adminapi.job.presentation.request.CrawlerJobRegistrationRequest
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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "크롤러 채용공고")
@RequestMapping("/api/v1/internal/jobs")
interface CrawlerJobApi {

    @Operation(
        summary = "크롤러 채용공고 등록",
        description = """
            크롤러가 수집한 채용공고를 게시 상태로 등록합니다.

            모집 기간 유형은 모집 시작·종료 일시가 모두 없으면 상시 채용으로, 하나라도 있으면 기간 채용으로 결정합니다.
            경력 연수를 모두 생략하면 경력 무관, 요구 학력을 생략하면 학력 무관으로 등록합니다.
            이미 같은 원문 URL로 등록된 미삭제 공고가 있으면 409로 거절합니다.
        """,
    )
    @SecurityRequirement(name = ADMIN_INTERNAL_API_KEY_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 요청 값이 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 내부 API 키가 없거나 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "JOB_ALREADY_EXISTS: 이미 등록된 원문 URL입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping
    fun registerJob(
        @Valid @RequestBody request: CrawlerJobRegistrationRequest,
    ): ResponseEntity<SuccessResponse<CrawlerJobRegistrationResponse>>
}
