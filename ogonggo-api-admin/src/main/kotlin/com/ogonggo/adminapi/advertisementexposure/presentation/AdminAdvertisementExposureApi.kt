package com.ogonggo.adminapi.advertisementexposure.presentation

import com.ogonggo.adminapi.advertisementexposure.presentation.request.CreateAdminAdvertisementExposureRequest
import com.ogonggo.adminapi.advertisementexposure.presentation.request.ChangeAdminAdvertisementExposureExposedRequest
import com.ogonggo.adminapi.advertisementexposure.presentation.request.MoveAdminAdvertisementExposureRequest
import com.ogonggo.adminapi.advertisementexposure.presentation.request.UpdateAdminAdvertisementExposureRequest
import com.ogonggo.adminapi.advertisementexposure.presentation.response.AdminAdvertisementExposureResponse
import com.ogonggo.adminapi.advertisementexposure.presentation.response.CreateAdminAdvertisementExposureResponse
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

private const val EXPOSURE_BAD_REQUEST = "BAD_REQUEST: 필수 값, ID, URL, 기간 또는 상품 카테고리별 내용이 올바르지 않습니다."
private const val EXPOSURE_NOT_FOUND = "ADVERTISEMENT_EXPOSURE_NOT_FOUND: 광고 노출을 찾을 수 없습니다."
private const val PRODUCT_NOT_FOUND = "ADVERTISEMENT_PRODUCT_NOT_FOUND: 광고 상품을 찾을 수 없습니다."
private const val CONTRACT_NOT_FOUND = "ADVERTISEMENT_CONTRACT_NOT_FOUND: 광고 계약을 찾을 수 없습니다."

@Tag(name = "관리자 광고 노출")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdvertisementExposureApi {

    @Operation(
        operationId = "listAdvertisementExposures",
        summary = "상품 자리의 광고 노출 목록 조회",
        description = "미삭제 노출을 displayOrder와 ID 오름차순으로 페이지 조회합니다. page는 1부터 시작하고 size는 1~100이며 기본값은 1과 10입니다. 꺼짐·기간 전·기간 종료 항목과 삭제된 상품 자리의 항목도 포함합니다. contractId를 생략하면 자체 홍보를 포함한 전체 노출을 반환합니다. advertiserCompanyName은 현재 계약의 companyName이며 자체 홍보는 null입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: productId와 contractId는 양수여야 합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun getExposures(
        @Parameter(
            description = "조회할 상품 자리 ID",
            required = true,
            schema = Schema(type = "integer", format = "int64"),
        )
        @Positive productId: Long,
        @Parameter(
            description = "계약 필터. 생략하면 모든 노출과 자체 홍보를 반환합니다.",
            schema = Schema(type = "integer", format = "int64"),
        )
        @Positive contractId: Long?,
        @Parameter(description = "1부터 시작하는 페이지 번호", example = "1") @Min(1) page: Int,
        @Parameter(description = "페이지 크기(1~100)", example = "10") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminAdvertisementExposureResponse>>>

    @Operation(
        operationId = "createAdvertisementExposure",
        summary = "광고 노출 등록",
        description = "상품 category에 따라 BANNER 콘텐츠 또는 JOB_POSTING 대상을 등록합니다. category, displayOrder와 슬롯은 요청에 포함하지 않습니다. contractId는 선택이며 존재만 확인합니다. JOB_POSTING target의 실제 존재·게시 상태는 확인하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "노출 생성", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = EXPOSURE_BAD_REQUEST + " ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE, ADVERTISEMENT_BANNER_CONTENT_REQUIRED, ADVERTISEMENT_JOB_POSTING_TARGET_REQUIRED, ADVERTISEMENT_EXPOSURE_PERIOD_INVALID",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "$PRODUCT_NOT_FOUND $CONTRACT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun createExposure(
        @Valid request: CreateAdminAdvertisementExposureRequest,
    ): ResponseEntity<SuccessResponse<CreateAdminAdvertisementExposureResponse>>

    @Operation(
        operationId = "replaceAdvertisementExposure",
        summary = "광고 노출 내용·계약·기간 교체",
        description = "미삭제 노출의 계약, 카테고리별 콘텐츠, 광고 표시와 기간을 전체 교체합니다. contractId·targetType·targetId·기간은 생략하거나 null로 보내면 null이며, BANNER의 name·imageUrl·linkUrl은 필수입니다. productId, exposed와 displayOrder는 변경하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "노출 수정", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "$EXPOSURE_BAD_REQUEST ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE, ADVERTISEMENT_BANNER_CONTENT_REQUIRED, ADVERTISEMENT_JOB_POSTING_TARGET_REQUIRED, ADVERTISEMENT_EXPOSURE_PERIOD_INVALID",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "$EXPOSURE_NOT_FOUND $PRODUCT_NOT_FOUND $CONTRACT_NOT_FOUND",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun updateExposure(
        @Parameter(description = "수정할 노출 ID", required = true, schema = Schema(type = "integer", format = "int64"))
        @Positive exposureId: Long,
        @Valid request: UpdateAdminAdvertisementExposureRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "updateAdvertisementExposureExposed",
        summary = "광고 노출 켜기·끄기",
        description = "exposed 값만 변경합니다. 같은 값도 성공하며, 상품·기간·계약·순서는 변경하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "노출 상태 변경", useReturnTypeSchema = true),
            ApiResponse(responseCode = "400", description = "BAD_REQUEST: exposureId는 양수이고 exposed는 필수 boolean이어야 합니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = EXPOSURE_NOT_FOUND, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun changeExposureExposed(
        @Parameter(description = "상태를 바꿀 노출 ID", required = true, schema = Schema(type = "integer", format = "int64"))
        @Positive exposureId: Long,
        @Valid request: ChangeAdminAdvertisementExposureExposedRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "moveAdvertisementExposure",
        summary = "같은 상품 자리에서 광고 노출 이동",
        description = "같은 상품의 모든 미삭제 노출을 기준으로 인접 항목과 순서를 교환합니다. 계약 필터와 무관하며 경계에서 이동하면 순서를 바꾸지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "노출 이동", useReturnTypeSchema = true),
            ApiResponse(responseCode = "400", description = "BAD_REQUEST: exposureId는 양수이고 direction은 UP 또는 DOWN이어야 합니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = EXPOSURE_NOT_FOUND, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun moveExposure(
        @Parameter(description = "이동할 노출 ID", required = true, schema = Schema(type = "integer", format = "int64"))
        @Positive exposureId: Long,
        @Valid request: MoveAdminAdvertisementExposureRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteAdvertisementExposure",
        summary = "광고 노출 삭제",
        description = "미삭제 노출을 소프트 삭제합니다. 다른 노출의 displayOrder는 다시 매기지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "노출 삭제", useReturnTypeSchema = true),
            ApiResponse(responseCode = "400", description = "BAD_REQUEST: exposureId는 양수여야 합니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = EXPOSURE_NOT_FOUND, content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다.", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    fun deleteExposure(
        @Parameter(description = "삭제할 노출 ID", required = true, schema = Schema(type = "integer", format = "int64"))
        @Positive exposureId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
