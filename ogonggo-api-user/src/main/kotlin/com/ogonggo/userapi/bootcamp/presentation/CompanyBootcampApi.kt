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

@Tag(name = "기업회원 부트캠프")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface CompanyBootcampApi {

    @Operation(operationId = "createMyBootcamp", summary = "부트캠프 등록")
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
    fun createBootcamp(
        @Parameter(hidden = true) userId: Long,
        @Valid request: CreateCompanyBootcampRequest,
    ): ResponseEntity<SuccessResponse<CreateCompanyBootcampResponse>>

    @Operation(operationId = "listMyBootcamps", summary = "내 부트캠프 목록 조회")
    fun getBootcamps(
        @Parameter(hidden = true) userId: Long,
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<CompanyBootcampSummaryResponse>>>

    @Operation(operationId = "getMyBootcamp", summary = "내 부트캠프 상세 조회")
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
    fun getBootcamp(
        @Parameter(hidden = true) userId: Long,
        @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<CompanyBootcampDetailResponse>>

    @Operation(operationId = "replaceMyBootcamp", summary = "내 부트캠프 수정")
    fun updateBootcamp(
        @Parameter(hidden = true) userId: Long,
        @Positive bootcampId: Long,
        @Valid request: UpdateCompanyBootcampRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(operationId = "startMyBootcampRecruitment", summary = "부트캠프 모집 시작")
    fun startRecruitment(
        @Parameter(hidden = true) userId: Long,
        @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(operationId = "closeMyBootcamp", summary = "부트캠프 모집 마감")
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
    fun closeBootcamp(
        @Parameter(hidden = true) userId: Long,
        @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(operationId = "deleteMyBootcamp", summary = "내 부트캠프 삭제")
    fun deleteBootcamp(
        @Parameter(hidden = true) userId: Long,
        @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
