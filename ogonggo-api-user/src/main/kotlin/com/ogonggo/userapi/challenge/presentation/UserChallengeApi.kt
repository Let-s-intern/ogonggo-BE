package com.ogonggo.userapi.challenge.presentation

import com.ogonggo.userapi.challenge.presentation.response.UserRecommendedChallengeResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "렛츠커리어 챌린지")
interface UserChallengeApi {

    @Operation(
        operationId = "listPublicRecommendedChallenges",
        summary = "추천 렛츠커리어 챌린지 조회",
        description = """
            렛츠커리어에서 모집 중인 챌린지 중 최대 3개를 반환합니다. 페이지 정보는 없습니다.
            지금은 렛츠커리어가 무작위로 고르며, 사용자에 맞춘 추천으로 바뀌어도 이 계약은 그대로입니다.

            로그인 없이 조회할 수 있습니다. 액세스 토큰을 보내면 그 사용자의 렛츠커리어 계정을 추천에 넘깁니다.

            렛츠커리어가 응답하지 않으면 오류 대신 빈 목록으로 응답합니다. 빈 목록이면 추천 구역을 숨깁니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    fun getRecommendedChallenges(
        @Parameter(hidden = true)
        userId: Long?,
    ): ResponseEntity<SuccessResponse<List<UserRecommendedChallengeResponse>>>
}
