package com.ogonggo.adminapi.adchannelformat.presentation

import com.ogonggo.adminapi.adchannelformat.presentation.request.CreateAdminAdChannelFormatRequest
import com.ogonggo.adminapi.adchannelformat.presentation.request.ReplaceAdminAdChannelFormatOrderRequest
import com.ogonggo.adminapi.adchannelformat.presentation.request.UpdateAdminAdChannelFormatRequest
import com.ogonggo.adminapi.adchannelformat.presentation.response.AdminAdChannelFormatIdResponse
import com.ogonggo.adminapi.adchannelformat.presentation.response.AdminAdChannelFormatResponse
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

private const val FORMAT_NOT_FOUND_DESCRIPTION = "AD_CHANNEL_FORMAT_NOT_FOUND: 게재 유형을 찾을 수 없습니다."
private const val FORMAT_BAD_REQUEST_DESCRIPTION =
    "BAD_REQUEST: 화면에 그대로 보이는 `필드: 설명` 문구입니다. 예: name: 같은 플랫폼에 같은 이름의 게재 유형이 있습니다. / " +
        "defaultPrice: null 이거나 0 이상의 정수(원)로 보내 주세요. / channelIds: 같은 플랫폼에 있는 채널 id 만 보내 주세요. / " +
        "platform: 등록한 뒤에는 바꿀 수 없습니다."

@Tag(name = "관리자 채널 광고 게재 유형")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdChannelFormatApi {

    @Operation(
        operationId = "listAdChannelFormats",
        summary = "게재 유형 목록 조회",
        description = "모든 플랫폼의 유형입니다. 사용 안 함도 넣고 플랫폼 순(INSTAGRAM, KAKAO_OPENCHAT), 그 안에서 displayOrder 순입니다.",
    )
    fun getFormats(): ResponseEntity<SuccessResponse<List<AdminAdChannelFormatResponse>>>

    @Operation(operationId = "createAdChannelFormat", summary = "게재 유형 등록", description = "그 플랫폼의 맨 뒤 순서로 더합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = FORMAT_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createFormat(
        @Valid request: CreateAdminAdChannelFormatRequest,
    ): ResponseEntity<SuccessResponse<AdminAdChannelFormatResponse>>

    @Operation(
        operationId = "replaceAdChannelFormatOrder",
        summary = "게재 유형 순서 바꾸기",
        description = "한 플랫폼의 유형 순서를 통째로 바꾸고 모든 유형 목록을 돌려줍니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: formatIds: 그 플랫폼의 게재 유형 id 를 빠짐없이, 겹치지 않게 보내 주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceOrder(
        @Valid request: ReplaceAdminAdChannelFormatOrderRequest,
    ): ResponseEntity<SuccessResponse<List<AdminAdChannelFormatResponse>>>

    @Operation(
        operationId = "updateAdChannelFormat",
        summary = "게재 유형 수정",
        description = "보낸 칸만 바꿉니다. 검증은 저장된 값과 합친 결과로 합니다. platform을 다른 값으로 보내면 400입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = FORMAT_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = FORMAT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateFormat(
        @Positive formatId: Long,
        @Valid request: UpdateAdminAdChannelFormatRequest,
    ): ResponseEntity<SuccessResponse<AdminAdChannelFormatResponse>>

    @Operation(
        operationId = "deleteAdChannelFormat",
        summary = "게재 유형 삭제",
        description = "채널 광고가 잡혀 있으면 400입니다. 지우면 같은 플랫폼의 남은 순서를 1부터 다시 매기고 이 유형을 산 계약의 " +
            "channelFormatIds에서도 뺍니다. 이미 지운 유형을 다시 지워도 200입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 이 유형으로 잡힌 채널 광고가 있어 지울 수 없습니다. 사용 안 함으로 바꿔 주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = FORMAT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteFormat(
        @Positive formatId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdChannelFormatIdResponse>>
}
