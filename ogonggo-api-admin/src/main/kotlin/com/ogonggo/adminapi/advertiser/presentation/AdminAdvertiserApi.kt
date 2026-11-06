package com.ogonggo.adminapi.advertiser.presentation

import com.ogonggo.adminapi.advertiser.presentation.request.CreateAdminAdvertiserRequest
import com.ogonggo.adminapi.advertiser.presentation.request.UpdateAdminAdvertiserRequest
import com.ogonggo.adminapi.advertiser.presentation.response.AdminAdvertiserResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.advertiser.domain.AdvertiserType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

private const val ADVERTISER_NOT_FOUND_DESCRIPTION = "ADVERTISER_NOT_FOUND: 광고주를 찾을 수 없습니다."

@Tag(name = "관리자 광고주")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdvertiserApi {

    @Operation(
        operationId = "listAdvertisers",
        summary = "광고주 목록 조회",
        description = """
            비즈니스 회원(MEMBER·COMPANY_MEMBER), 광고 문의 기업(NON_MEMBER·AD_INQUIRY), 직접 추가한 기업(NON_MEMBER·MANUAL)을
            합친 목록입니다. 탈퇴한 회원은 뺍니다. 광고주 행이 없는 회원·문의 기업은 이때 만들어 고정 id를 줍니다.

            진행 중 계약이 있는 곳, 계약만 있는 곳, 계약이 없는 곳 순이고, 같은 무리 안에서는 계약 endAt이 가장 늦은 곳,
            그다음 이름(한국어) 순입니다. 정렬한 뒤 최대 50건을 주며 페이지는 없습니다.

            keyword는 이름·담당자 이름·담당자 메일에서 대소문자를 가리지 않고 부분 일치로 찾습니다. type은 MEMBER 또는 NON_MEMBER입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: type: MEMBER, NON_MEMBER 중 하나를 보내 주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getAdvertisers(
        @Size(max = 100) keyword: String?,
        @Parameter(schema = Schema(implementation = AdvertiserType::class)) type: String?,
    ): ResponseEntity<SuccessResponse<List<AdminAdvertiserResponse>>>

    @Operation(operationId = "getAdvertiser", summary = "광고주 상세 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = ADVERTISER_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getAdvertiser(
        @Positive advertiserId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserResponse>>

    @Operation(
        operationId = "createAdvertiser",
        summary = "비회원 광고주 추가",
        description = "운영자가 직접 더하는 비회원 광고주(source = MANUAL)입니다. 공백·대소문자를 빼고 같은 이름의 광고주가 있으면 400입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "추가 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: name: 1~50자로 보내 주세요. / name: 같은 이름의 광고주가 이미 있습니다. 목록에서 골라 주세요. / " +
                    "contactEmail: 메일 주소 형식으로 보내 주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createAdvertiser(
        @Valid request: CreateAdminAdvertiserRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserResponse>>

    @Operation(
        operationId = "updateAdvertiser",
        summary = "비회원 광고주 수정",
        description = """
            보낸 칸만 바꾸고 빈 문자열은 그 칸을 지웁니다. 칸을 모두 본 뒤 한꺼번에 바꾸므로 하나라도 걸리면 아무것도 바뀌지 않습니다.
            같은 이름은 자기 자신을 빼고 봅니다. 회원 광고주는 비즈니스 회원 정보를 따르므로 400입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 회원 광고주는 비즈니스 회원 정보를 따릅니다. 회원 정보에서 바꿔 주세요. / " +
                    "name: 같은 이름의 광고주가 이미 있습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = ADVERTISER_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateAdvertiser(
        @Positive advertiserId: Long,
        @Valid request: UpdateAdminAdvertiserRequest,
    ): ResponseEntity<SuccessResponse<AdminAdvertiserResponse>>
}
