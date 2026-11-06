package com.ogonggo.adminapi.banner.presentation

import com.ogonggo.adminapi.banner.presentation.request.CreateAdminBannerRequest
import com.ogonggo.adminapi.banner.presentation.request.ReplaceAdminBannerOrderRequest
import com.ogonggo.adminapi.banner.presentation.request.UpdateAdminBannerRequest
import com.ogonggo.adminapi.banner.presentation.response.AdminBannerIdResponse
import com.ogonggo.adminapi.banner.presentation.response.AdminBannerResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.banner.domain.BannerPlacement
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
import org.springframework.http.ResponseEntity

private const val BANNER_NOT_FOUND_DESCRIPTION = "BANNER_NOT_FOUND: 배너를 찾을 수 없습니다."
private const val BANNER_BAD_REQUEST_DESCRIPTION =
    "BAD_REQUEST: 화면에 그대로 보이는 `필드: 설명` 문구입니다. 예: linkUrl: http(s):// 주소나 / 로 시작하는 경로를 보내 주세요. / " +
        "endAt: 노출 끝은 시작보다 뒤여야 합니다. / advertiserId: 광고 표시가 켜진 배너는 광고주를 골라 주세요. / " +
        "mobileImageUrl: 상세 사이드 배너는 모바일 이미지(750×200)가 있어야 합니다. / contractId: 고른 광고주의 계약만 이을 수 있습니다."

@Tag(name = "관리자 배너")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminBannerApi {

    @Operation(
        operationId = "listBanners",
        summary = "배너 목록 조회",
        description = """
            placement를 주면 그 자리의 배너 전부(꺼진 것·기간이 지난 것 포함)를 displayOrder 순서로 줍니다. 페이지는 없습니다.
            advertiserId를 주면 그 광고주의 배너만 줍니다. placement 없이 advertiserId만 주면 모든 자리에서 자리 순서
            (HOME_TOP, HOME_BOTTOM, JOB_DETAIL_SIDE, BOOTCAMP_TOP, BOOTCAMP_BOTTOM, BOOTCAMP_DETAIL_SIDE)·자리 안 순서대로 줍니다.
            둘 다 없으면 400입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: placement: ... 중 하나를 보내 주세요. / advertiserId: 광고주 id 를 숫자로 보내 주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getBanners(
        @Parameter(schema = Schema(implementation = BannerPlacement::class))
        placement: String?,
        @Parameter(description = "광고주 id(숫자)", schema = Schema(type = "integer", format = "int64"))
        advertiserId: String?,
    ): ResponseEntity<SuccessResponse<List<AdminBannerResponse>>>

    @Operation(
        operationId = "createBanner",
        summary = "배너 등록",
        description = "그 자리 맨 뒤(displayOrder = 그 자리 개수 + 1)에 붙습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = BANNER_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createBanner(
        @Valid request: CreateAdminBannerRequest,
    ): ResponseEntity<SuccessResponse<AdminBannerResponse>>

    @Operation(
        operationId = "replaceBannerOrder",
        summary = "자리 안 배너 순서 바꾸기",
        description = "bannerIds는 그 자리의 배너 id를 빠짐없이 한 번씩 새 순서대로 담아야 합니다. 응답은 그 자리 배너 전부(새 순서)입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: bannerIds: 그 자리의 배너 id 를 빠짐없이 한 번씩 보내 주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceBannerOrder(
        @Valid request: ReplaceAdminBannerOrderRequest,
    ): ResponseEntity<SuccessResponse<List<AdminBannerResponse>>>

    @Operation(
        operationId = "updateBanner",
        summary = "배너 수정",
        description = "보낸 칸만 바꿉니다. 자리는 바꿀 수 없고, 검증은 저장된 값과 합친 결과로 합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = BANNER_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = BANNER_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateBanner(
        @Positive bannerId: Long,
        @Valid request: UpdateAdminBannerRequest,
    ): ResponseEntity<SuccessResponse<AdminBannerResponse>>

    @Operation(
        operationId = "deleteBanner",
        summary = "배너 삭제",
        description = "지우고 그 자리에 남은 배너의 순서를 1부터 다시 매깁니다. 이미 지운 배너를 다시 지워도 200입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = BANNER_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteBanner(
        @Positive bannerId: Long,
    ): ResponseEntity<SuccessResponse<AdminBannerIdResponse>>
}
