package com.ogonggo.userapi.advertisementevent.presentation

import com.ogonggo.userapi.advertisementevent.presentation.response.AdvertisementContractReportResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import java.time.LocalDate

@Tag(name = "광고")
interface AdvertisementContractReportApi {

    @Operation(
        operationId = "getAdvertisementContractReport",
        summary = "내 광고 계약 성과 리포트 조회",
        description = "로그인한 기업 회원 본인의 계약만 조회할 수 있습니다. 날짜는 Asia/Seoul 기준이며 최근 1년 범위만 받습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(responseCode = "400", description = "BAD_REQUEST: 리포트 기간이 올바르지 않습니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "ADVERTISEMENT_REPORT_FORBIDDEN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "ADVERTISEMENT_CONTRACT_NOT_FOUND", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "503", description = "ADVERTISEMENT_REPORT_SOURCE_UNAVAILABLE", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun getReport(
        userId: Long,
        @Parameter(description = "조회 시작일, Asia/Seoul 기준", required = true) from: LocalDate,
        @Parameter(description = "조회 종료일, Asia/Seoul 기준", required = true) to: LocalDate,
        contractId: Long,
    ): ResponseEntity<SuccessResponse<AdvertisementContractReportResponse>>
}
