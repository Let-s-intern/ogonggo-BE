package com.ogonggo.adminapi.letscareercontent.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.letscareercontent.presentation.request.ReplaceAdminLetsCareerContentRecommendWeightsRequest
import com.ogonggo.adminapi.letscareercontent.presentation.response.AdminLetsCareerContentRecommendWeightsResponse
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
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 렛츠커리어 콘텐츠 추천")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminLetsCareerContentRecommendApi {

    @Operation(
        operationId = "getLetsCareerContentRecommendWeights",
        summary = "렛츠커리어 콘텐츠 추천 점수 조회",
        description = """
            채용공고 상세의 렛츠커리어 콘텐츠 추천(공고마다 3개)에 쓰는 점수를 줍니다. 바꾼 적이 없으면 기본값입니다.

            콘텐츠마다 직무 일치·직군 일치·준비 단계 일치 점수를 더하고, 그 합이 0보다 크면 갈래(프로그램·자료집·블로그) 가산점을 더합니다.
            점수가 높은 순으로 고르되 한 갈래는 maxPerGroup개까지만 고릅니다. 다른 직군 전용 콘텐츠는 점수와 상관없이 뺍니다.
        """,
    )
    fun getWeights(): ResponseEntity<SuccessResponse<AdminLetsCareerContentRecommendWeightsResponse>>

    @Operation(
        operationId = "replaceLetsCareerContentRecommendWeights",
        summary = "렛츠커리어 콘텐츠 추천 점수 수정",
        description = """
            추천 점수 전체를 바꾸고 바뀐 값을 반환합니다. 다음 추천 요청부터 바로 씁니다. 같은 요청을 반복해도 결과가 같습니다.
            프로그램을 더 보이게 하려면 programBonus를 올리거나 maxPerGroup을 3으로 둡니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 칸이 빠졌거나 범위를 벗어났습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceWeights(
        @Valid request: ReplaceAdminLetsCareerContentRecommendWeightsRequest,
    ): ResponseEntity<SuccessResponse<AdminLetsCareerContentRecommendWeightsResponse>>
}
