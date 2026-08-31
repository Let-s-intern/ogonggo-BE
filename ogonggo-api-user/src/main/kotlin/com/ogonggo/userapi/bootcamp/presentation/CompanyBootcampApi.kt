package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.userapi.bootcamp.presentation.request.CreateCompanyBootcampRequest
import com.ogonggo.userapi.bootcamp.presentation.request.UpdateCompanyBootcampRequest
import com.ogonggo.userapi.bootcamp.presentation.response.CompanyBootcampDetailResponse
import com.ogonggo.userapi.bootcamp.presentation.response.CompanyBootcampSummaryResponse
import com.ogonggo.userapi.bootcamp.presentation.response.CreateCompanyBootcampResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
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
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "기업회원 부트캠프")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
@RequestMapping("/api/v1/users/me/bootcamps")
interface CompanyBootcampApi {

    @Operation(summary = "부트캠프 등록")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "403",
                description = "COMPANY_ROLE_REQUIRED, USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping
    fun createBootcamp(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: CreateCompanyBootcampRequest,
    ): ResponseEntity<SuccessResponse<CreateCompanyBootcampResponse>>

    @Operation(summary = "내 부트캠프 목록 조회")
    @GetMapping
    fun getBootcamps(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") @Min(1) page: Int,
        @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<CompanyBootcampSummaryResponse>>>

    @Operation(summary = "내 부트캠프 상세 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "BOOTCAMP_NOT_FOUND: 부트캠프를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/{bootcampId}")
    fun getBootcamp(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<CompanyBootcampDetailResponse>>

    @Operation(summary = "내 부트캠프 수정")
    @PutMapping("/{bootcampId}")
    fun updateBootcamp(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") @Positive bootcampId: Long,
        @RequestBody @Valid request: UpdateCompanyBootcampRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(summary = "부트캠프 모집 시작")
    @PostMapping("/{bootcampId}/start-recruitment")
    fun startRecruitment(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(summary = "부트캠프 모집 마감")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "마감 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "409",
                description = "INVALID_BOOTCAMP_STATUS_TRANSITION: 허용되지 않는 상태 변경",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/{bootcampId}/close")
    fun closeBootcamp(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(summary = "내 부트캠프 삭제")
    @DeleteMapping("/{bootcampId}")
    fun deleteBootcamp(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("bootcampId") @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
