package com.ogonggo.userapi.advertisementevent.presentation

import com.ogonggo.userapi.advertisementevent.presentation.request.CreateAdvertisementEventRequest
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.enums.ParameterIn
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
import java.util.UUID

@Tag(name = "광고")
interface AdvertisementEventApi {

    @Operation(
        operationId = "createAdvertisementEvent",
        summary = "광고 노출·클릭 기록",
        description = "비회원도 호출할 수 있습니다. 익명 ID는 항상 필수이며, 로그인 회원은 선택적으로 Bearer 토큰을 보내면 회원 ID를 연결합니다. Idempotency-Key에는 이벤트별 UUID를 보내고, 같은 이벤트를 재시도할 때 같은 값을 재사용합니다. 실제 화면에 보인 노출 또는 클릭을 기록하고, 같은 회원·노출의 노출 이벤트는 한국 시간 기준 하루 한 번만 집계하며 서로 다른 키의 클릭은 각각 기록합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "기록 성공 또는 같은 이벤트 키·같은 날 노출 중복으로 무시", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: Idempotency-Key가 UUID가 아니거나 요청값이 올바르지 않거나 현재 노출 중인 광고가 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(responseCode = "404", description = "ADVERTISEMENT_EXPOSURE_NOT_FOUND", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ],
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun createEvent(
        userId: Long?,
        @Parameter(
            name = "Idempotency-Key",
            `in` = ParameterIn.HEADER,
            description = "이벤트별 UUID. 같은 이벤트의 네트워크 재시도에는 동일한 값을 재사용합니다.",
            required = true,
        ) idempotencyKey: UUID,
        @Valid request: CreateAdvertisementEventRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
