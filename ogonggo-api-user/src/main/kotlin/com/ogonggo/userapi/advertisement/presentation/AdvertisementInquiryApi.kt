package com.ogonggo.userapi.advertisement.presentation

import com.ogonggo.userapi.advertisement.presentation.request.CreateAdvertisementInquiryRequest
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "광고 문의")
@RequestMapping("/api/v1/advertisement-inquiries")
interface AdvertisementInquiryApi {

    @Operation(
        summary = "B2B 광고 문의 접수",
        description = "기업 담당자가 광고 소개 페이지에서 남긴 문의를 영업 슬랙 채널로 전달합니다. " +
            "문의를 저장하지 않아 생성되는 리소스가 없으므로 201이 아닌 200과 `data: null`로 응답합니다. " +
            "슬랙 전달에 실패하면 문의가 남지 않으므로 성공으로 응답하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "접수 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "503",
                description = "ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED: 문의를 영업 채널로 전달하지 못했습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping
    fun createInquiry(
        @RequestBody
        @Valid
        request: CreateAdvertisementInquiryRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
