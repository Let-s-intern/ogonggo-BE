package com.ogonggo.userapi.advertisementcontract.presentation

import com.ogonggo.userapi.advertisementcontract.presentation.response.MyActiveAdvertisementReportResponse
import com.ogonggo.userapi.advertisementcontract.presentation.response.MyAdvertisementReportResponse
import com.ogonggo.userapi.advertisementcontract.presentation.response.MyAdvertisementReportTotalResponse
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
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import java.time.LocalDate

private const val REPORT_DESCRIPTION = """
    기업 회원이 자기 광고 계약에 붙은 상품 하나의 성과 리포트를 봅니다. 광고 신청 폼 계약과 비즈니스 공고 계약 모두 됩니다.
    summary·daily·exposures는 이 상품의 노출로 들어온 이벤트만 센 상품 몫이고, targets는 계약 대상 공고의 전체 숫자입니다.
    전날까지는 일별 집계, 오늘은 원본에서 셉니다. from·to(yyyy-MM-dd, 포함)는 상품 기간 안으로 자르며 비우면 상품 기간 전체(진행 중이면 오늘까지)입니다.
    배너 상품은 스크롤 깊이·체류 시간이 null입니다.
"""
private const val FORBIDDEN = "COMPANY_ROLE_REQUIRED, USER_SUSPENDED 또는 USER_WITHDRAWN"
private const val NOT_FOUND =
    "ADVERTISEMENT_CONTRACT_NOT_FOUND: 내 계약이 아니거나 없습니다. ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND: 계약에 붙은 상품이 아닙니다."
private const val BAD_REQUEST = "BAD_REQUEST: Path ID나 날짜 형식이 올바르지 않습니다. REPORT_PERIOD_INVALID: 조회 기간이 상품 기간 밖이거나 from이 to보다 늦습니다."
private const val NOT_AVAILABLE = "REPORT_NOT_AVAILABLE: 기간이 없거나 오공고 화면에 노출하지 않는 채널 상품이라 리포트가 없습니다."

@Tag(name = "광고 성과 리포트")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface MyAdvertisementReportApi {

    @Operation(operationId = "getMyAdvertisementContractProductReport", summary = "내 광고 상품 리포트 조회", description = REPORT_DESCRIPTION)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(responseCode = "400", description = BAD_REQUEST, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = FORBIDDEN, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = NOT_FOUND, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = NOT_AVAILABLE, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun getReport(
        @Parameter(hidden = true) userId: Long,
        @Positive contractId: Long,
        @Positive contractProductId: Long,
        @Parameter(description = "조회 시작일(포함). 비우면 상품 시작일") from: LocalDate?,
        @Parameter(description = "조회 종료일(포함). 비우면 상품 종료일과 오늘 중 이른 날") to: LocalDate?,
    ): ResponseEntity<SuccessResponse<MyAdvertisementReportResponse>>

    @Operation(
        operationId = "getMyAdvertisementContractReportSummary",
        summary = "내 광고 계약 전체 합계 조회",
        description = """
            계약에 붙은 상품 중 리포트를 열 수 있는 상품의 상품 몫을 더합니다. 각 상품은 상품 기간 전체(진행 중이면 오늘까지)이고,
            아직 시작하지 않은 상품은 0입니다. 기간이 없거나 채널 상품은 products에 넣지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(responseCode = "400", description = "BAD_REQUEST: contractId는 양수여야 합니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = FORBIDDEN, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(
                responseCode = "404",
                description = "ADVERTISEMENT_CONTRACT_NOT_FOUND: 내 계약이 아니거나 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getReportSummary(
        @Parameter(hidden = true) userId: Long,
        @Positive contractId: Long,
    ): ResponseEntity<SuccessResponse<MyAdvertisementReportTotalResponse>>

    @Operation(
        operationId = "listMyActiveAdvertisementReports",
        summary = "진행 중 광고 요약 조회",
        description = """
            대시보드 카드용입니다. 내 모든 계약에서 오늘이 상품 기간 안이고 리포트를 열 수 있는 상품을 끝나는 날이 가까운 순으로 줍니다.
            숫자는 상품 시작일부터 오늘까지의 상품 몫이며 상품 리포트의 summary와 같은 정의입니다. 없으면 빈 배열입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = FORBIDDEN, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun getActiveReports(
        @Parameter(hidden = true) userId: Long,
    ): ResponseEntity<SuccessResponse<List<MyActiveAdvertisementReportResponse>>>

    @Operation(
        operationId = "downloadMyAdvertisementContractProductReportCsv",
        summary = "내 광고 상품 리포트 CSV 다운로드",
        description = """
            리포트와 같은 기간·숫자를 날짜별·소재별 한 줄로 내려받습니다. UTF-8(BOM 포함) CSV이며 열은
            date,exposureId,exposureName,impressions,adClicks,views,uniqueVisitors,memberViews,ctaClicks,ctaUniqueVisitors,avgScrollPercent,avgDurationSeconds
            입니다. 값이 없는 평균은 빈 칸입니다. 오류는 JSON 오류 응답입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "CSV 본문", content = [Content(mediaType = "text/csv", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "400", description = BAD_REQUEST, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = FORBIDDEN, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = NOT_FOUND, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = NOT_AVAILABLE, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun getReportCsv(
        @Parameter(hidden = true) userId: Long,
        @Positive contractId: Long,
        @Positive contractProductId: Long,
        @Parameter(description = "조회 시작일(포함)") from: LocalDate?,
        @Parameter(description = "조회 종료일(포함)") to: LocalDate?,
    ): ResponseEntity<String>
}
