package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.presentation.request.ReplaceMyProfileRequest
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
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "내 정보")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
@RequestMapping("/api/v1/users/me/profile")
interface UserProfileApi {

    @Operation(
        summary = "내 프로필 수정",
        description = """
            학력(대학교·전공·학년)과 희망 조건(직군·직무·산업·구직 조건·희망 기업)을 교체합니다.
            조회는 내 정보 조회(GET /api/v1/users/me)의 profile에 함께 담깁니다.

            여덟 값을 함께 교체하므로 보내지 않은 값은 비웁니다.
            일부만 바꿀 때도 바꾸지 않을 값을 함께 보내야 합니다.

            이름·닉네임·프로필 이미지는 렛츠커리어가 소유해 로그인마다 갱신되므로 여기서 바꿀 수 없습니다.
            반대로 이 여덟 값은 오공고가 소유해 재로그인해도 덮어쓰지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 학력은 30자, 희망 조건은 1000자를 넘을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "USER_PROFILE_CONFLICT: 같은 사용자의 저장이 동시에 들어와 실패했습니다. 재시도하면 성공합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PutMapping
    fun replaceMyProfile(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: Long,
        @RequestBody
        @Valid
        request: ReplaceMyProfileRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
