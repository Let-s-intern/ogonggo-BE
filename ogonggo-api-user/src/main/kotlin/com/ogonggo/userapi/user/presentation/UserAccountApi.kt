package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.presentation.response.MyAccountResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "내 정보")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
@RequestMapping("/api/v1/users/me")
interface UserAccountApi {

    @Operation(
        summary = "내 정보 조회",
        description = """
            로그인한 사용자의 역할과 프로필을 반환합니다.

            액세스 토큰에는 역할이 들어 있지 않으므로 기업 회원 화면을 열지 판단하려면 이 API의 role을 사용합니다.
            role이 COMPANY이면 companyProfile이, 그 밖이면 profile이 채워지고 반대쪽은 null입니다.

            정지·탈퇴한 계정도 403이 아니라 200으로 응답하며 status에 현재 상태가 담깁니다.
            액세스 토큰은 상태가 바뀌어도 만료까지 유효하므로, 다른 요청이 왜 막히는지 이 값으로 판단합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "USER_NOT_FOUND: 사용자를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping
    fun getMyAccount(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: Long,
    ): ResponseEntity<SuccessResponse<MyAccountResponse>>
}
