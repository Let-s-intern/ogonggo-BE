package com.ogonggo.userapi.user.presentation

import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.SuccessResponse
import com.ogonggo.userapi.user.presentation.request.ReplaceMyCompanyBasicInfoRequest
import com.ogonggo.userapi.user.presentation.request.ReplaceMyCompanyManagerInfoRequest
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
interface CompanyProfileApi {

    @Operation(
        operationId = "replaceMyCompanyBasicInfo",
        summary = "기업 기본 정보 수정",
        description = """
            기업 회원의 기관명과 기업 로고를 함께 교체합니다.
            조회는 내 정보 조회(GET /api/v1/users/me)의 companyProfile에 함께 담깁니다.

            로고는 이미지 업로드(POST /api/v1/images)로 파일을 올리고 응답의 id를 logoImageId로 보냅니다.
            로고를 바꾸지 않을 때도 companyProfile.logoImageId를 그대로 보내야 하며, 빼거나 null로 보내면 로고를 지웁니다.
            바꾸거나 지운 이전 로고 이미지는 하루 뒤 지워집니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = """
                    BAD_REQUEST: 기관명은 비어 있거나 150자를 넘을 수 없고, logoImageId는 공백이거나 36자를 넘을 수 없습니다.
                    IMAGE_ASSET_NOT_AVAILABLE: 내가 올린 이미지가 아니거나, 게시글에 쓰는 중이거나, 이미 지워진 이미지입니다.
                """,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "COMPANY_ROLE_REQUIRED, USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceMyCompanyBasicInfo(
        @Parameter(hidden = true)
        userId: Long,
        @Valid
        request: ReplaceMyCompanyBasicInfoRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "replaceMyCompanyManagerInfo",
        summary = "기업 담당자 정보 수정",
        description = """
            기업 회원의 담당자 이름, 연락처, 오늘의 공고 정보 수신용 이메일을 함께 교체합니다.
            조회는 내 정보 조회(GET /api/v1/users/me)의 companyProfile에 함께 담깁니다.

            연락처·수신 이메일은 선택 값이며, 빼거나 null로 보내면 저장된 값을 비웁니다.
            로그인 이메일과 비밀번호는 여기서 바꿀 수 없습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 담당자 이름(100자)은 비어 있거나 길이를 넘을 수 없습니다. " +
                    "연락처는 숫자·하이픈(20자), 수신 이메일은 이메일 형식(320자)이며 공백일 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "COMPANY_ROLE_REQUIRED, USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceMyCompanyManagerInfo(
        @Parameter(hidden = true)
        userId: Long,
        @Valid
        request: ReplaceMyCompanyManagerInfoRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
