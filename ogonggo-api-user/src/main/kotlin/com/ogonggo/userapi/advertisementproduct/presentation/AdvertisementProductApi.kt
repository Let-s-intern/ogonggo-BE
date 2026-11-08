package com.ogonggo.userapi.advertisementproduct.presentation

import com.ogonggo.userapi.advertisementproduct.presentation.response.AdvertisementProductResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity

@Tag(name = "광고")
interface AdvertisementProductApi {

    @Operation(
        operationId = "listPublicAdvertisementProducts",
        summary = "광고 신청 상품 목록 조회",
        description = "신청 화면에 표시할 삭제되지 않은 상품 정보를 카테고리와 상품 ID 순으로 페이지 조회합니다. page는 1부터 시작하고 size는 1~100이며 기본값은 1과 10입니다. 신청 계약에는 이 응답의 상품 ID가 아니라 광고 카테고리가 저장됩니다. 비회원도 조회할 수 있습니다.",
    )
    fun getProducts(
        @Parameter(description = "1부터 시작하는 페이지 번호", example = "1") @Min(1) page: Int,
        @Parameter(description = "페이지 크기(1~100)", example = "10") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<AdvertisementProductResponse>>>
}
