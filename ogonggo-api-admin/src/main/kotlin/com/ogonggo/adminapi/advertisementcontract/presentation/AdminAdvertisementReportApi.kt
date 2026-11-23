package com.ogonggo.adminapi.advertisementcontract.presentation

import com.ogonggo.adminapi.advertisementcontract.presentation.request.ReplaceAdminAdvertisementReportInsightsRequest
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminAdvertisementReportInsightResponse
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminAdvertisementReportResponse
import com.ogonggo.adminapi.advertisementcontract.presentation.response.AdminAdvertisementReportTotalResponse
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
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import java.time.LocalDate

private const val NOT_FOUND =
    "ADVERTISEMENT_CONTRACT_NOT_FOUND: 광고 계약을 찾을 수 없습니다. ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND: 계약에 붙은 상품이 아닙니다."
private const val BAD_REQUEST = "BAD_REQUEST: Path ID나 날짜 형식이 올바르지 않습니다. REPORT_PERIOD_INVALID: 조회 기간이 상품 기간 밖이거나 from이 to보다 늦습니다."
private const val NOT_AVAILABLE = "REPORT_NOT_AVAILABLE: 기간이 없거나 오공고 화면에 노출하지 않는 채널 상품이라 리포트가 없습니다."

@Tag(name = "관리자 광고 성과 리포트")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdvertisementReportApi {

    @Operation(
        operationId = "getAdvertisementContractProductReport",
        summary = "광고 상품 리포트 조회",
        description = """
            계약에 붙은 상품 하나의 성과 리포트입니다. 기업 회원이 보는 리포트와 같은 숫자입니다.
            summary·daily·exposures는 이 상품의 노출로 들어온 이벤트만 센 상품 몫이고, targets는 계약 대상 공고의 전체 숫자입니다.
            from·to(yyyy-MM-dd, 포함)는 상품 기간 안으로 자르며 비우면 상품 기간 전체(진행 중이면 오늘까지)입니다. 배너 상품은 스크롤 깊이·체류 시간이 null입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(responseCode = "400", description = BAD_REQUEST, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = NOT_FOUND, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = NOT_AVAILABLE, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun getReport(
        @Positive contractId: Long,
        @Positive contractProductId: Long,
        @Parameter(description = "조회 시작일(포함). 비우면 상품 시작일") from: LocalDate?,
        @Parameter(description = "조회 종료일(포함). 비우면 상품 종료일과 오늘 중 이른 날") to: LocalDate?,
    ): ResponseEntity<SuccessResponse<AdminAdvertisementReportResponse>>

    @Operation(
        operationId = "replaceAdvertisementReportInsights",
        summary = "광고 상품 리포트 인사이트 저장",
        description = """
            계약 상품 하나의 인사이트(제목·본문)를 통째로 바꿉니다. 목록 순서가 보이는 순서이고 빈 목록이면 모두 걷어냅니다.
            published가 true이면 기업 회원의 리포트에 보내고, false이면 운영자만 보는 임시 저장입니다(이미 보낸 것도 숨겨집니다).
            인사이트는 최대 6개, 제목 60자·본문 500자까지입니다. 운영자는 리포트 조회 응답의 insights로 임시 저장까지 읽습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "저장 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: Path ID가 양수가 아니거나 인사이트가 6개를 넘고 제목·본문이 비었거나 너무 깁니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = NOT_FOUND, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun replaceInsights(
        @Positive contractId: Long,
        @Positive contractProductId: Long,
        @Valid request: ReplaceAdminAdvertisementReportInsightsRequest,
    ): ResponseEntity<SuccessResponse<List<AdminAdvertisementReportInsightResponse>>>

    @Operation(
        operationId = "getAdvertisementContractReportSummary",
        summary = "광고 계약 전체 합계 조회",
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
            ApiResponse(responseCode = "403", description = "FORBIDDEN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(
                responseCode = "404",
                description = "ADVERTISEMENT_CONTRACT_NOT_FOUND: 광고 계약을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getReportSummary(@Positive contractId: Long): ResponseEntity<SuccessResponse<AdminAdvertisementReportTotalResponse>>

    @Operation(
        operationId = "downloadAdvertisementContractProductReportCsv",
        summary = "광고 상품 리포트 CSV 다운로드",
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
            ApiResponse(responseCode = "403", description = "FORBIDDEN", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = NOT_FOUND, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = NOT_AVAILABLE, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun getReportCsv(
        @Positive contractId: Long,
        @Positive contractProductId: Long,
        @Parameter(description = "조회 시작일(포함)") from: LocalDate?,
        @Parameter(description = "조회 종료일(포함)") to: LocalDate?,
    ): ResponseEntity<String>
}
