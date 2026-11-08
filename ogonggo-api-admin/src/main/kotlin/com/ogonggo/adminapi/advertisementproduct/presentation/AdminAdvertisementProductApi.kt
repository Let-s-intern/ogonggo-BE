package com.ogonggo.adminapi.advertisementproduct.presentation

import com.ogonggo.adminapi.advertisementproduct.presentation.request.CreateAdminAdvertisementProductRequest
import com.ogonggo.adminapi.advertisementproduct.presentation.request.ReplaceAdminAdvertisementProductRequest
import com.ogonggo.adminapi.advertisementproduct.presentation.response.AdminAdvertisementProductResponse
import com.ogonggo.adminapi.advertisementproduct.presentation.response.CreateAdminAdvertisementProductResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
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
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import org.springframework.http.ResponseEntity

private const val PRODUCT_NOT_FOUND_DESCRIPTION = "ADVERTISEMENT_PRODUCT_NOT_FOUND: 광고 상품을 찾을 수 없습니다."
private const val PRODUCT_POSITION_MISMATCH_DESCRIPTION = "ADVERTISEMENT_PRODUCT_POSITION_MISMATCH: 노출 자리가 상품 카테고리와 맞지 않습니다."

@Tag(name = "관리자 광고 상품")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdvertisementProductApi {

    @Operation(
        operationId = "listAdvertisementProducts",
        summary = "광고 상품 목록 조회",
        description = "삭제되지 않은 광고 상품을 카테고리와 상품 ID 순으로 페이지 조회합니다. page는 1부터 시작하고 size는 1~100이며 기본값은 1과 10입니다.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
        ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "FORBIDDEN: 관리자 권한 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ])
    fun getProducts(
        @Parameter(description = "1부터 시작하는 페이지 번호", example = "1") @Min(1) page: Int,
        @Parameter(description = "페이지 크기(1~100)", example = "10") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminAdvertisementProductResponse>>>

    @Operation(
        operationId = "createAdvertisementProduct",
        summary = "광고 상품 등록",
        description = "광고 상품을 등록합니다. position은 category에 해당하는 배너 자리일 때 지정하고, price는 소개 가격입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 필수값·문자열 길이·가격이 올바르지 않습니다. $PRODUCT_POSITION_MISMATCH_DESCRIPTION",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 관리자 권한 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun createProduct(
        @Valid request: CreateAdminAdvertisementProductRequest,
    ): ResponseEntity<SuccessResponse<CreateAdminAdvertisementProductResponse>>

    @Operation(
        operationId = "replaceAdvertisementProduct",
        summary = "광고 상품 전체 수정",
        description = "상품의 category, position, name, price를 교체합니다. position과 price를 누락하거나 null로 보내면 값을 해제합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 필수값·문자열 길이·가격이 올바르지 않습니다. $PRODUCT_POSITION_MISMATCH_DESCRIPTION",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = PRODUCT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 관리자 권한 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun replaceProduct(
        @Positive productId: Long,
        @Valid request: ReplaceAdminAdvertisementProductRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteAdvertisementProduct",
        summary = "광고 상품 삭제",
        description = "상품을 소프트 삭제합니다. 계약 및 노출 기록은 유지하고, 이미 삭제되었거나 없는 상품에는 404를 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = PRODUCT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 관리자 권한 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun deleteProduct(
        @Positive productId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
