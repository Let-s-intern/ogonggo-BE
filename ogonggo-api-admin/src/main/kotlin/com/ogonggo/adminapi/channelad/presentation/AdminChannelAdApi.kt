package com.ogonggo.adminapi.channelad.presentation

import com.ogonggo.adminapi.channelad.presentation.request.CreateAdminChannelAdRequest
import com.ogonggo.adminapi.channelad.presentation.request.UpdateAdminChannelAdRequest
import com.ogonggo.adminapi.channelad.presentation.response.AdminChannelAdIdResponse
import com.ogonggo.adminapi.channelad.presentation.response.AdminChannelAdResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.channelad.domain.ChannelAdStatus
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
import org.springframework.http.ResponseEntity

private const val CHANNEL_AD_NOT_FOUND_DESCRIPTION = "CHANNEL_AD_NOT_FOUND: 채널 광고를 찾을 수 없습니다."
private const val CHANNEL_AD_BAD_REQUEST_DESCRIPTION =
    "BAD_REQUEST: 화면에 그대로 보이는 `필드: 설명` 문구입니다. 예: advertiserId: 있는 광고주를 골라 주세요. / " +
        "contractId: 고른 광고주의 계약만 이을 수 있습니다. / channelId: 사용 안 함인 채널은 고를 수 없습니다. / " +
        "formatId: 채널과 같은 플랫폼의 게재 유형을 골라 주세요. / " +
        "endDate: null 이거나 게시 예정일과 같거나 뒤인 YYYY-MM-DD 로 보내 주세요."

@Tag(name = "관리자 채널 광고")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminChannelAdApi {

    @Operation(
        operationId = "listChannelAds",
        summary = "채널 광고 목록 조회",
        description = "게시일, id 순입니다. 모든 조건은 선택이고 비우면 전부입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: month: YYYY-MM 로 보내 주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getChannelAds(
        @Parameter(description = "YYYY-MM. 그달과 겹치는 것(postDate ~ endDate ?? postDate가 그달에 걸침)", example = "2026-11")
        month: String?,
        @Parameter(description = "광고주") advertiserId: Long?,
        @Parameter(description = "광고 계약") contractId: Long?,
        @Parameter(description = "채널의 플랫폼") platform: AdChannelPlatform?,
        @Parameter(description = "채널") channelId: Long?,
        @Parameter(description = "게재 유형") formatId: Long?,
        @Parameter(description = "진행 상태") status: ChannelAdStatus?,
    ): ResponseEntity<SuccessResponse<List<AdminChannelAdResponse>>>

    @Operation(operationId = "createChannelAd", summary = "채널 광고 등록")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = CHANNEL_AD_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createChannelAd(
        @Valid request: CreateAdminChannelAdRequest,
    ): ResponseEntity<SuccessResponse<AdminChannelAdResponse>>

    @Operation(
        operationId = "updateChannelAd",
        summary = "채널 광고 수정",
        description = "보낸 칸만 바꿉니다. 검증은 저장된 값과 합친 결과로 합니다. 광고주만 바꾸고 contractId를 보내지 않으면 계약을 끊습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = CHANNEL_AD_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CHANNEL_AD_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateChannelAd(
        @Positive adId: Long,
        @Valid request: UpdateAdminChannelAdRequest,
    ): ResponseEntity<SuccessResponse<AdminChannelAdResponse>>

    @Operation(operationId = "deleteChannelAd", summary = "채널 광고 삭제", description = "이미 지운 광고를 다시 지워도 200입니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = CHANNEL_AD_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteChannelAd(
        @Positive adId: Long,
    ): ResponseEntity<SuccessResponse<AdminChannelAdIdResponse>>
}
