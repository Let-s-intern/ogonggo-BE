package com.ogonggo.adminapi.featureflag.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.featureflag.presentation.request.UpdateFeatureFlagRequest
import com.ogonggo.adminapi.featureflag.presentation.response.AdminFeatureFlagListResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
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

@Tag(name = "관리자 기능 플래그")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminFeatureFlagApi {

    @Operation(
        operationId = "listFeatureFlags",
        summary = "기능 플래그 목록 조회",
        description = """
            모든 기능 플래그를 key 순으로 반환합니다. 사용자 API의 공개 목록(`GET /api/v1/feature-flags`)과 달리 설명, 마지막으로 바꾼 일시, 바꾼 관리자를 함께 줍니다.
            플래그를 새로 만드는 API는 없습니다. 새 플래그는 그 플래그를 쓰는 기능의 배포 SQL로 들어옵니다.
        """,
    )
    fun getFeatureFlags(): ResponseEntity<SuccessResponse<AdminFeatureFlagListResponse>>

    @Operation(
        operationId = "updateFeatureFlag",
        summary = "기능 플래그 켜기·끄기",
        description = """
            플래그를 켜거나 끄고 요청한 관리자를 바꾼 사람으로 남깁니다. 같은 요청을 반복해도 결과가 같고, 이미 그 값이어도 바꾼 관리자는 요청한 관리자로 남습니다.
            성공하면 `data`는 null이며 바뀐 값은 목록을 다시 조회해 확인합니다.
            사용자 API는 목록을 서버마다 30초 캐시하므로 사용자 화면에는 최대 30초 뒤에 반영됩니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: enabled가 빠졌거나 참·거짓이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "FEATURE_FLAG_NOT_FOUND: key에 해당하는 플래그가 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateFeatureFlag(
        @Parameter(hidden = true) adminUserId: Long,
        @Parameter(description = "플래그 key", example = "ad_report_cost_efficiency") key: String,
        @Valid request: UpdateFeatureFlagRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
