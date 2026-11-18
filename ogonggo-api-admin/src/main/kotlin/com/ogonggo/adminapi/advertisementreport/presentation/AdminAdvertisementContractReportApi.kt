package com.ogonggo.adminapi.advertisementreport.presentation

import com.ogonggo.adminapi.advertisementreport.presentation.response.AdminAdvertisementContractReportResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
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

@Tag(name = "관리자 광고")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdvertisementContractReportApi {

    @Operation(
        operationId = "getAdminAdvertisementContractReport",
        summary = "광고 계약 성과 리포트 조회",
        description = "계약의 최근 1년 이내 일별·지면별 성과와 클릭 회원 직무 분포를 조회합니다. 날짜 범위는 Asia/Seoul 기준입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(responseCode = "400", description = "BAD_REQUEST: 리포트 기간이 올바르지 않습니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 활성 관리자 권한이 없습니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "ADVERTISEMENT_CONTRACT_NOT_FOUND", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun getReport(
        @Parameter(description = "계약 식별자", required = true) contractId: Long,
        @Parameter(description = "조회 시작일, Asia/Seoul 기준", required = true) from: LocalDate,
        @Parameter(description = "조회 종료일, Asia/Seoul 기준", required = true) to: LocalDate,
    ): ResponseEntity<SuccessResponse<AdminAdvertisementContractReportResponse>>

    @Operation(
        operationId = "exportAdminAdvertisementContractReportCsv",
        summary = "광고 계약 성과 리포트 CSV 내보내기",
        description = "관리자에게 계약의 일별·지면별 성과와 클릭 회원 직무 분포를 UTF-8 CSV로 내려줍니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "CSV 파일", content = [Content(mediaType = "text/csv")]),
            ApiResponse(responseCode = "400", description = "BAD_REQUEST: 리포트 기간이 올바르지 않습니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 활성 관리자 권한이 없습니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "ADVERTISEMENT_CONTRACT_NOT_FOUND", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun exportReportCsv(
        @Parameter(description = "계약 식별자", required = true) contractId: Long,
        @Parameter(description = "조회 시작일, Asia/Seoul 기준", required = true) from: LocalDate,
        @Parameter(description = "조회 종료일, Asia/Seoul 기준", required = true) to: LocalDate,
    ): ResponseEntity<ByteArray>
}
