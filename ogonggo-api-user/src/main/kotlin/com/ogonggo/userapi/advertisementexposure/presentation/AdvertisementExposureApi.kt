package com.ogonggo.userapi.advertisementexposure.presentation

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.userapi.advertisementexposure.presentation.response.AdvertisementExposureResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity

@Tag(name = "광고 노출")
interface AdvertisementExposureApi {

    @Operation(
        operationId = "listPublicAdvertisementExposures",
        summary = "자리별 공개 배너 조회",
        description = "비회원도 조회할 수 있습니다. 현재 AdvertisementProductPosition의 배너 자리 6종만 받으며, 켜져 있고 노출 기간 안인 배너를 displayOrder와 노출 ID 순으로 페이지 조회합니다. page는 1부터 시작하고 size는 1~100이며 기본값은 1과 10입니다. linkUrl은 노출 등록 때 허용한 HTTP(S) 절대 URL 또는 단일 슬래시 내부 경로를 저장된 값 그대로 반환합니다. mobileImageUrl은 모바일 배너 이미지이며 null이면 모바일에서도 imageUrl을 씁니다. 광고주·상품·계약·대상 식별 정보와 기간 등 공개 계약에 없는 값은 반환하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 필수 position이 없거나 현재 배너 자리 enum 값이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getShown(
        @Parameter(description = "고정된 배너 자리 이름", required = true)
        position: AdvertisementProductPosition,
        @Parameter(description = "1부터 시작하는 페이지 번호", example = "1") @Min(1) page: Int,
        @Parameter(description = "페이지 크기(1~100)", example = "10") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<AdvertisementExposureResponse>>>
}
