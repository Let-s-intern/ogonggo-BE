package com.ogonggo.userapi.advertisementproduct.presentation

import com.ogonggo.userapi.advertisementproduct.presentation.response.AdvertisementProductResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "광고 상품")
interface AdvertisementProductApi {

    @Operation(
        operationId = "listPublicAdvertisementProducts",
        summary = "광고 신청 상품 목록 조회",
        description = "신청 화면에 표시할 삭제되지 않은 상품 정보를 카테고리와 상품 ID 순으로 반환합니다. 신청 계약에는 이 응답의 상품 ID가 아니라 광고 카테고리가 저장됩니다. 비회원도 조회할 수 있습니다.",
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)])
    fun getProducts(): ResponseEntity<SuccessResponse<List<AdvertisementProductResponse>>>
}
