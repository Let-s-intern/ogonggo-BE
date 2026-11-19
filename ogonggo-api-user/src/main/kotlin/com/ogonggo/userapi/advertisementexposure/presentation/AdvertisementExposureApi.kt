package com.ogonggo.userapi.advertisementexposure.presentation

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.userapi.advertisementexposure.presentation.response.AdvertisementExposureResponse
import com.ogonggo.userapi.advertisementexposure.presentation.response.AdvertisementPostingResponse
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
import org.springframework.http.ResponseEntity

@Tag(name = "광고 노출")
interface AdvertisementExposureApi {

    @Operation(
        operationId = "listPublicAdvertisementExposures",
        summary = "자리별 공개 배너 조회",
        description = "비회원도 조회할 수 있습니다. 현재 AdvertisementProductPosition의 배너 자리 6종만 받으며(광고 공고 자리는 400, 광고 공고는 GET /api/v1/advertisement-exposures/postings), 켜져 있고 노출 기간 안인 배너를 displayOrder와 노출 ID 순으로 페이지 조회합니다. page는 1부터 시작하고 size는 1~100이며 기본값은 1과 10입니다. linkUrl은 노출 등록 때 허용한 HTTP(S) 절대 URL 또는 단일 슬래시 내부 경로를 저장된 값 그대로 반환합니다. mobileImageUrl은 모바일 배너 이미지이며 null이면 모바일에서도 imageUrl을 씁니다. 광고주·상품·계약·대상 식별 정보와 기간 등 공개 계약에 없는 값은 반환하지 않습니다.",
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
        @Parameter(
            description = "고정된 배너 자리 이름",
            required = true,
            schema = Schema(
                type = "string",
                allowableValues = [
                    "JOB_HOME_TOP_BANNER",
                    "JOB_HOME_BOTTOM_BANNER",
                    "JOB_DETAIL_BANNER",
                    "BOOTCAMP_LIST_TOP_BANNER",
                    "BOOTCAMP_LIST_BOTTOM_BANNER",
                    "BOOTCAMP_DETAIL_BANNER",
                ],
            ),
        )
        position: AdvertisementProductPosition,
        @Parameter(description = "1부터 시작하는 페이지 번호", example = "1") @Min(1) page: Int,
        @Parameter(description = "페이지 크기(1~100)", example = "10") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<AdvertisementExposureResponse>>>

    @Operation(
        operationId = "listPublicAdvertisementPostings",
        summary = "자리별 공개 광고 공고 조회",
        description = "로그인 없이 호출할 수 있고, 토큰을 보내면 요약의 bookmarked가 채워집니다. 광고 공고 자리(JOB_PICK, JOB_LIST, CALENDAR_DAY, MYPAGE_SCRAP)만 받습니다. 켜져 있고 노출 기간 안인 노출 중 대상이 게시 중이고 모집 중인 것만 displayOrder와 노출 ID 순으로 모두 반환하며 페이지를 나누지 않습니다. targetType이 JOB이면 job에 공개 채용공고 목록 요약이, BOOTCAMP이면 bootcamp에 공개 부트캠프 목록 요약이 있고 나머지 하나는 null입니다. 광고주·상품·계약·기간은 반환하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 필수 position이 없거나 광고 공고 자리가 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun getShownPostings(
        @Parameter(hidden = true) userId: Long?,
        @Parameter(
            description = "고정된 광고 공고 자리 이름",
            required = true,
            schema = Schema(type = "string", allowableValues = ["JOB_PICK", "JOB_LIST", "CALENDAR_DAY", "MYPAGE_SCRAP"]),
        )
        position: AdvertisementProductPosition,
    ): ResponseEntity<SuccessResponse<List<AdvertisementPostingResponse>>>
}
