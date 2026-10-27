package com.ogonggo.adminapi.advertisementinquiry.presentation

import com.ogonggo.adminapi.advertisementinquiry.presentation.response.AdminAdvertisementInquiryResponse
import com.ogonggo.adminapi.advertisementinquiry.presentation.response.AdminAdvertisementInquiryTodayCountResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import java.time.LocalDate

@Tag(name = "관리자 광고 문의")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdvertisementInquiryApi {

    @Operation(
        operationId = "listAdvertisementInquiries",
        summary = "광고 문의 목록 조회",
        description = """
            기업 담당자가 남긴 광고 문의를 최근에 들어온 순으로 반환합니다. 정렬은 고를 수 없습니다.
            문의 유형·캠페인 코드·처리 상태·회원 구분·유입 경로로 거를 수 있고 필터는 모두 AND로 묶입니다.
            createdFrom·createdTo는 접수일(YYYY-MM-DD, 한국 시간) 범위이며 두 날짜를 모두 포함합니다.
            값을 보내지 않거나 빈 값을 보내면 그 조건을 적용하지 않습니다. 현재 조건의 건수는 pageInfo.totalElements입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 페이지 범위나 필터 값이 올바르지 않거나, 접수 기간 시작일이 종료일보다 늦습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getAdvertisementInquiries(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        inquiryType: AdvertisementInquiryType?,
        @Parameter(description = "캠페인 코드. 일치하는 값만 거릅니다.", example = "launch_free_promo") @Size(max = 50) campaignCode: String?,
        status: AdvertisementInquiryStatus?,
        memberType: AdvertisementInquiryMemberType?,
        inflowChannel: AdvertisementInquiryInflowChannel?,
        @Parameter(description = "접수일 시작(포함)") createdFrom: LocalDate?,
        @Parameter(description = "접수일 끝(포함)") createdTo: LocalDate?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminAdvertisementInquiryResponse>>>

    @Operation(
        operationId = "getAdvertisementInquiryTodayCount",
        summary = "오늘 들어온 광고 문의 수 조회",
        description = """
            관리자 메뉴 숫자용입니다. 한국 시간 오늘 0시부터 지금까지 들어온 광고 문의 수를 반환합니다.
            문의 유형과 처리 상태를 가리지 않으므로 처리 상태를 바꿔도 줄지 않습니다.
        """,
    )
    fun getTodayCount(): ResponseEntity<SuccessResponse<AdminAdvertisementInquiryTodayCountResponse>>
}
