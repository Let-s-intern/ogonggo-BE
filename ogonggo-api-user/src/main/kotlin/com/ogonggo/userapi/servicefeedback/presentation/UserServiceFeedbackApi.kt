package com.ogonggo.userapi.servicefeedback.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.servicefeedback.presentation.request.CreateServiceFeedbackRequest
import com.ogonggo.userapi.servicefeedback.presentation.response.CreateServiceFeedbackResponse
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

@Tag(name = "서비스 개선 의견")
interface UserServiceFeedbackApi {

    @Operation(
        operationId = "createServiceFeedback",
        summary = "서비스 개선 의견 작성",
        description = """
            만족스러운 점(satisfaction)과 아쉬운 점(improvement)을 남깁니다. 두 문항 모두 선택이지만 하나 이상은 채워야 하며,
            공백만 있는 문항은 비운 것으로 봅니다. 각 문항은 1000자 이하입니다.

            로그인 없이 작성할 수 있습니다. 액세스 토큰을 보내면 작성자를 함께 기록합니다.
            한 사용자가 여러 번 작성할 수 있으며 작성할 때마다 새 의견이 생깁니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "작성 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 두 문항이 모두 비었거나 1000자를 넘습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun createServiceFeedback(
        @Parameter(hidden = true)
        userId: Long?,
        @Valid
        request: CreateServiceFeedbackRequest,
    ): ResponseEntity<SuccessResponse<CreateServiceFeedbackResponse>>
}
