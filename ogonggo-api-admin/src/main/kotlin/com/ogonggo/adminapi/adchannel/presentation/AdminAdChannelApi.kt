package com.ogonggo.adminapi.adchannel.presentation

import com.ogonggo.adminapi.adchannel.presentation.request.CreateAdminAdChannelRequest
import com.ogonggo.adminapi.adchannel.presentation.request.UpdateAdminAdChannelRequest
import com.ogonggo.adminapi.adchannel.presentation.response.AdminAdChannelIdResponse
import com.ogonggo.adminapi.adchannel.presentation.response.AdminAdChannelResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

private const val CHANNEL_NOT_FOUND_DESCRIPTION = "AD_CHANNEL_NOT_FOUND: 채널을 찾을 수 없습니다."
private const val CHANNEL_BAD_REQUEST_DESCRIPTION =
    "BAD_REQUEST: 화면에 그대로 보이는 `필드: 설명` 문구입니다. 예: handle: 이미 등록된 계정입니다. / " +
        "name: 1~50자로 보내 주세요. / url: http:// 또는 https:// 로 시작하는 주소를 보내 주세요. / " +
        "platform: 등록한 뒤에는 바꿀 수 없습니다."

@Tag(name = "관리자 채널 광고 채널")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdChannelApi {

    @Operation(
        operationId = "listAdChannels",
        summary = "채널 목록 조회",
        description = "모든 플랫폼의 채널입니다. 사용 안 함도 넣고 등록 순으로 둡니다.",
    )
    fun getChannels(): ResponseEntity<SuccessResponse<List<AdminAdChannelResponse>>>

    @Operation(operationId = "createAdChannel", summary = "채널 등록")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = CHANNEL_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createChannel(
        @Valid request: CreateAdminAdChannelRequest,
    ): ResponseEntity<SuccessResponse<AdminAdChannelResponse>>

    @Operation(
        operationId = "updateAdChannel",
        summary = "채널 수정",
        description = "보낸 칸만 바꿉니다. 검증은 저장된 값과 합친 결과로 합니다. platform을 다른 값으로 보내면 400입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = CHANNEL_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CHANNEL_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateChannel(
        @Positive channelId: Long,
        @Valid request: UpdateAdminAdChannelRequest,
    ): ResponseEntity<SuccessResponse<AdminAdChannelResponse>>

    @Operation(
        operationId = "deleteAdChannel",
        summary = "채널 삭제",
        description = "채널 광고가 하나라도 잡혀 있으면 400입니다. 지우면 게재 유형의 channelIds에서도 뺍니다. 이미 지운 채널을 다시 지워도 200입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 이 채널에 잡힌 채널 광고가 있어 지울 수 없습니다. 사용 안 함으로 바꿔 주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = CHANNEL_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteChannel(
        @Positive channelId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdChannelIdResponse>>
}
