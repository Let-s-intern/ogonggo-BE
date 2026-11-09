package com.ogonggo.userapi.advertisementcontract.presentation

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractStatus
import com.ogonggo.userapi.advertisementcontract.presentation.response.MyAdvertisementContractDetailResponse
import com.ogonggo.userapi.advertisementcontract.presentation.response.MyAdvertisementContractSummaryResponse
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
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

@Tag(name = "광고 신청 내역")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface MyAdvertisementContractApi {

    @Operation(
        operationId = "listMyAdvertisementContracts",
        summary = "내 광고 신청 목록 조회",
        description = """
            로그인한 회원이 광고 신청 폼으로 낸 신청을 최근 신청부터 조회합니다. 로그인 없이 낸 신청과
            비즈니스 공고 등록으로 생긴 계약은 넣지 않습니다. status를 여러 번 보내면 그중 하나인 신청을 찾습니다.
            page는 1부터 시작하고 기본 1, size 기본 10·최대 100입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 페이지 범위 또는 status 값이 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun getContracts(
        @Parameter(hidden = true) userId: Long,
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        @Parameter(description = "여러 번 전달하면 해당 상태 중 하나인 신청을 조회합니다.")
        statuses: List<AdvertisementContractStatus>?,
    ): ResponseEntity<SuccessResponse<PageResponse<MyAdvertisementContractSummaryResponse>>>

    @Operation(
        operationId = "getMyAdvertisementContract",
        summary = "내 광고 신청 상세 조회",
        description = """
            내가 낸 신청의 폼 입력값, 상태, 협의 금액과 운영자가 계약에 붙인 광고 상품을 조회합니다.
            상품 이름·카테고리·자리는 조회 시점의 상품 값이며, 운영자가 지운 상품도 productDeleted=true로 보입니다.
            다른 회원의 신청, 로그인 없이 낸 신청, 비즈니스 공고 계약은 없는 신청과 같이 404입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: contractId는 양수여야 합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(
                responseCode = "404",
                description = "ADVERTISEMENT_CONTRACT_NOT_FOUND: 광고 계약을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getContract(
        @Parameter(hidden = true) userId: Long,
        @Positive contractId: Long,
    ): ResponseEntity<SuccessResponse<MyAdvertisementContractDetailResponse>>
}
