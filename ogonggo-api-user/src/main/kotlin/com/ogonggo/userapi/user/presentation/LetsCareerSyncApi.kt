package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.config.USER_INTERNAL_API_KEY_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.presentation.request.LetsCareerJobProfileSyncRequest
import com.ogonggo.userapi.user.presentation.response.LetsCareerJobProfileSyncResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "렛츠커리어 연동")
@SecurityRequirement(name = USER_INTERNAL_API_KEY_SCHEME)
interface LetsCareerSyncApi {

    @Operation(
        operationId = "replaceLetsCareerJobProfile",
        summary = "[Internal] 렛츠커리어 학력·희망 조건 반영",
        description = """
            렛츠커리어 서버가 자기 쪽에서 고친 학력·희망 조건 전체를 보냅니다. null이면 비웁니다.
            경로의 식별자는 오공고가 아니라 렛츠커리어 사용자 식별자입니다.

            updatedAt(렛츠커리어에서 고친 일시)이 오공고의 최종 수정 일시보다 나중일 때만 반영하고 applied=true로 응답합니다.
            오공고에서 더 나중에 고쳤거나 이미 받은 수정이면 반영하지 않고 applied=false로 응답합니다.
            오공고 계정이 없는 렛츠커리어 사용자도 applied=false입니다.
            같은 요청을 여러 번 보내도 결과가 같습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 내부 API 키가 없거나 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "USER_PROFILE_CONFLICT: 같은 사용자의 프로필이 동시에 만들어져 실패했습니다. 재시도하면 성공합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceJobProfile(
        letsCareerUserId: Long,
        @Valid
        request: LetsCareerJobProfileSyncRequest,
    ): ResponseEntity<SuccessResponse<LetsCareerJobProfileSyncResponse>>
}
