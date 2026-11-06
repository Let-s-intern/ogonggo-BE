package com.ogonggo.userapi.banner.presentation

import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.userapi.banner.presentation.response.UserBannerResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "배너")
interface UserBannerApi {

    @Operation(
        operationId = "listPublicBanners",
        summary = "자리별 노출 중인 배너 조회",
        description = """
            그 자리에 지금 노출 중인(켜져 있고 기간 안인) 배너만 displayOrder 순서로 줍니다. 없으면 빈 배열입니다.
            로그인 없이 부르며 토큰을 보내도 응답은 같습니다. 서버가 자리마다 30초 캐시하므로 관리자 변경과 기간 경계는
            최대 30초 늦게 반영됩니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: placement가 없거나 모르는 값입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getBanners(
        placement: BannerPlacement,
    ): ResponseEntity<SuccessResponse<List<UserBannerResponse>>>
}
