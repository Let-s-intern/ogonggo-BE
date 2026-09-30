package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.presentation.request.ChangeMyPasswordRequest
import com.ogonggo.userapi.user.presentation.request.ReplaceMyNotificationEmailRequest
import com.ogonggo.userapi.user.presentation.request.ReplaceMyProfileImageRequest
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
            기업 회원은 기업 담당자 정보 수정(PUT /api/v1/users/me/company-profile/manager-info)의 notificationEmail을 씁니다.
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
        operationId = "replaceMyProfileImage",
        summary = "프로필 이미지 변경",
        description = """
            일반 회원의 프로필 이미지를 바꿉니다.
            이미지 업로드(POST /api/v1/images)로 파일을 올리고 응답의 id를 imageId로 보냅니다.
            바뀐 이미지는 내 정보 조회(GET /api/v1/users/me)의 profile.profileImageUrl에 담깁니다.

            오공고 전용 값이라 렛츠커리어에는 보내지 않고, 다시 로그인해도 렛츠커리어 이미지로 돌아가지 않습니다.
            바꾸기 전 이미지는 하루 뒤 지워집니다. 같은 imageId를 다시 보내도 결과가 같습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = """
                    BAD_REQUEST: imageId가 비었거나 36자를 넘습니다.
                    IMAGE_ASSET_NOT_AVAILABLE: 내가 올린 이미지가 아니거나, 게시글에 쓰는 중이거나, 이미 지워진 이미지입니다.
                """,
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
    fun replaceMyProfileImage(
        @Parameter(hidden = true)
        userId: Long,
        @Valid
        request: ReplaceMyProfileImageRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteMyProfileImage",
        summary = "프로필 이미지 삭제",
        description = """
            오공고에서 바꾼 프로필 이미지를 지우고 렛츠커리어 프로필 이미지로 되돌립니다.
            렛츠커리어에도 이미지가 없으면 profile.profileImageUrl은 null입니다.
            바꾼 적이 없어도 200으로 응답합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "403",
                description = "GENERAL_MEMBER_REQUIRED: 기업 회원은 이 경로를 쓰지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteMyProfileImage(
        @Parameter(hidden = true)
        userId: Long,
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
