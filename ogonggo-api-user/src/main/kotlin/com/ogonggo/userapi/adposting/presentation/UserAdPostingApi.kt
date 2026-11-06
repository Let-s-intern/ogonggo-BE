package com.ogonggo.userapi.adposting.presentation

import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.userapi.adposting.presentation.response.UserAdPostingResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "광고 공고")
interface UserAdPostingApi {

    @Operation(
        operationId = "listPublicAdPostings",
        summary = "자리별 광고 공고 조회",
        description = """
            그 자리의 노출 중인 광고 공고(켜짐, 기간 안) 가운데 대상이 공개이고 모집 중인 것만 자리 안 순서대로 줍니다.
            마감 처리됐거나 모집 종료 일시가 지난 대상, 모집 중이 아닌 부트캠프는 뺍니다. 없으면 빈 배열입니다.
            카드를 그릴 대상 요약(job 또는 bootcamp)을 함께 싣습니다.

            로그인 없이 부르며 토큰을 보내도 응답은 같습니다. 대상 요약의 bookmarked는 늘 false입니다.
            자리마다 30초 캐시하므로 관리자 콘솔에서 바꾼 값과 조회 수·북마크 수는 길어야 30초 늦게 반영됩니다.
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
    fun getAdPostings(
        @Parameter(
            required = true,
            schema = Schema(implementation = AdPostingPlacement::class),
        )
        placement: String?,
    ): ResponseEntity<SuccessResponse<List<UserAdPostingResponse>>>
}
