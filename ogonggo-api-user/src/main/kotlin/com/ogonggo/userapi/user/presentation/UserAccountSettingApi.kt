package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.presentation.request.ChangeMyPasswordRequest
import com.ogonggo.userapi.user.presentation.request.ReplaceMyNotificationEmailRequest
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

@Tag(name = "내 정보")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface UserAccountSettingApi {

    @Operation(
        operationId = "replaceMyNotificationEmail",
        summary = "오늘의 공고 수신 이메일 수정",
        description = """
            일반 회원이 오늘의 공고 정보를 받을 이메일을 바꿉니다.
            조회는 내 정보 조회(GET /api/v1/users/me)의 profile.notificationEmail에 담깁니다.

            렛츠커리어 가입 이메일과 따로 두는 오공고 전용 값이라 렛츠커리어에는 보내지 않습니다.
            빼거나 null로 보내면 비웁니다.

            이름·휴대폰 번호·가입 이메일은 렛츠커리어가 소유해 오공고에서 바꿀 수 없습니다.
            기업 회원은 기업 정보 수정(PUT /api/v1/users/me/company-profile)의 notificationEmail을 씁니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 이메일 형식이 아니거나 공백입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "GENERAL_MEMBER_REQUIRED: 기업 회원은 이 경로를 쓰지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "USER_PROFILE_CONFLICT: 같은 사용자의 저장이 동시에 들어와 실패했습니다. 재시도하면 성공합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceMyNotificationEmail(
        @Parameter(hidden = true)
        userId: Long,
        @Valid
        request: ReplaceMyNotificationEmailRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "changeMyPassword",
        summary = "비밀번호 변경",
        description = """
            기존 비밀번호를 확인하고 새 비밀번호로 바꿉니다.

            일반 회원의 비밀번호는 렛츠커리어에 있으므로 렛츠커리어로 전달해 바꿉니다.
            렛츠커리어 로그인 비밀번호가 함께 바뀝니다.
            카카오·네이버·구글로 가입한 계정은 비밀번호가 없어 바꿀 수 없습니다.

            기업 회원은 오공고에서 바꿉니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = """
                    BAD_REQUEST: 값이 비었거나 새 비밀번호가 8~64자가 아닙니다.
                    CURRENT_PASSWORD_MISMATCH: 기존 비밀번호가 일치하지 않습니다.
                    INVALID_NEW_PASSWORD: 새 비밀번호에 특수문자가 없습니다(일반 회원).
                    SOCIAL_ACCOUNT_PASSWORD_UNAVAILABLE: 소셜 로그인으로 가입한 계정입니다.
                """,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "503",
                description = "LETSCAREER_UNAVAILABLE: 렛츠커리어 서버와 통신할 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun changeMyPassword(
        @Parameter(hidden = true)
        userId: Long,
        @Valid
        request: ChangeMyPasswordRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
