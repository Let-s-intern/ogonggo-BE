package com.ogonggo.userapi.advertisement.presentation

import com.ogonggo.userapi.advertisement.presentation.request.CreateAdvertisementInquiryRequest
import com.ogonggo.userapi.advertisement.presentation.response.CreateAdvertisementInquiryResponse
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
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "광고 문의")
interface AdvertisementInquiryApi {

    @Operation(
        operationId = "createAdvertisementInquiry",
        summary = "B2B 광고 문의 접수",
        description = """
            기업 담당자가 남긴 광고 문의를 저장하고 영업 슬랙 채널과 담당자 확인 메일로 알립니다.
            저장한 문의의 식별자를 201과 함께 돌려줍니다. 슬랙·메일 발송에 실패해도 저장은 남으므로 성공으로 응답합니다.

            로그인 없이 보낼 수 있습니다. 액세스 토큰을 보내면 그 회원과 회원 구분(기업 회원·일반 회원)을 서버가 정해 함께 저장하고,
            토큰이 없거나 유효하지 않으면 비회원으로 저장합니다. 요청 본문의 회원 구분·회원 식별자는 받지 않습니다.

            유입 경로는 utmSource와 referrer로 서버가 판정합니다. 캠페인 코드도 서버가 정합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "접수 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 필수값이 없거나 형식·길이가 맞지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun createInquiry(
        @Parameter(hidden = true)
        userId: Long?,
        @Valid
        request: CreateAdvertisementInquiryRequest,
    ): ResponseEntity<SuccessResponse<CreateAdvertisementInquiryResponse>>
}
