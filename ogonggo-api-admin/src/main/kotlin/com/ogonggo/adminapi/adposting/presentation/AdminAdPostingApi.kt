package com.ogonggo.adminapi.adposting.presentation

import com.ogonggo.adminapi.adposting.presentation.request.CreateAdminAdPostingRequest
import com.ogonggo.adminapi.adposting.presentation.request.ReplaceAdminAdPostingOrderRequest
import com.ogonggo.adminapi.adposting.presentation.request.UpdateAdminAdPostingRequest
import com.ogonggo.adminapi.adposting.presentation.response.AdminAdPostingIdResponse
import com.ogonggo.adminapi.adposting.presentation.response.AdminAdPostingResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.adposting.domain.AdPostingPlacement
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

private const val AD_POSTING_NOT_FOUND_DESCRIPTION = "AD_POSTING_NOT_FOUND: 광고 공고를 찾을 수 없습니다."
private const val PLACEMENT_BAD_REQUEST =
    "placement: JOB_LIST, JOB_PICK, BOOTCAMP_LIST, JOB_DETAIL_SIDE, CALENDAR, MYPAGE_SCRAP 중 하나를 보내 주세요."
private const val AD_POSTING_BAD_REQUEST_DESCRIPTION =
    "BAD_REQUEST: 화면에 그대로 보이는 `필드: 설명` 문구입니다. 예: $PLACEMENT_BAD_REQUEST / " +
        "targetId: 있는 채용공고 id 를 보내 주세요. / targetId: 이 자리에 이미 등록된 대상입니다. / " +
        "advertiserId: 광고주를 골라 주세요. / contractId: 있는 계약을 골라 주세요. / " +
        "contractId: 고른 광고주의 계약만 이을 수 있습니다. / endAt: 노출 끝은 시작보다 뒤여야 합니다."

@Tag(name = "관리자 광고 공고")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAdPostingApi {

    @Operation(
        operationId = "listAdPostings",
        summary = "광고 공고 목록 조회",
        description = """
            placement를 주면 그 자리의 광고 공고 전부(꺼진 것·기간이 지난 것·대상이 비공개인 것 포함)를 자리 안 순서대로 줍니다.
            advertiserId를 주면 그 광고주의 광고 공고만 주고, placement를 함께 주지 않으면 모든 자리에서 자리 순서
            (JOB_LIST, JOB_PICK, BOOTCAMP_LIST, JOB_DETAIL_SIDE, CALENDAR, MYPAGE_SCRAP)·자리 안 순서대로 줍니다.
            둘 다 없으면 400입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: $PLACEMENT_BAD_REQUEST / advertiserId: 광고주 id 를 숫자로 보내 주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getAdPostings(
        @Parameter(schema = Schema(implementation = AdPostingPlacement::class)) placement: String?,
        @Parameter(schema = Schema(type = "integer", format = "int64")) advertiserId: String?,
    ): ResponseEntity<SuccessResponse<List<AdminAdPostingResponse>>>

    @Operation(operationId = "createAdPosting", summary = "광고 공고 등록", description = "그 자리의 맨 뒤에 붙습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = AD_POSTING_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createAdPosting(
        @Valid request: CreateAdminAdPostingRequest,
    ): ResponseEntity<SuccessResponse<AdminAdPostingResponse>>

    @Operation(
        operationId = "updateAdPosting",
        summary = "광고 공고 수정",
        description = "보낸 칸(advertiserId, contractId, startAt, endAt, enabled)만 바꿉니다. 자리와 대상은 바꾸지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = AD_POSTING_BAD_REQUEST_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = AD_POSTING_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateAdPosting(
        @Positive adPostingId: Long,
        @Valid request: UpdateAdminAdPostingRequest,
    ): ResponseEntity<SuccessResponse<AdminAdPostingResponse>>

    @Operation(
        operationId = "deleteAdPosting",
        summary = "광고 공고 삭제",
        description = "지우고 그 자리에 남은 광고 공고의 순서를 1부터 다시 매깁니다. 이미 지운 광고 공고를 다시 지워도 200입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = AD_POSTING_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteAdPosting(
        @Positive adPostingId: Long,
    ): ResponseEntity<SuccessResponse<AdminAdPostingIdResponse>>

    @Operation(
        operationId = "replaceAdPostingOrder",
        summary = "광고 공고 자리 안 순서 바꾸기",
        description = "그 자리의 광고 공고 id를 빠짐없이 한 번씩 새 순서대로 보냅니다. 응답은 그 자리의 광고 공고 전부(새 순서)입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: $PLACEMENT_BAD_REQUEST / adPostingIds: 그 자리의 광고 id 를 빠짐없이 한 번씩 보내 주세요.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun replaceAdPostingOrder(
        @Valid request: ReplaceAdminAdPostingOrderRequest,
    ): ResponseEntity<SuccessResponse<List<AdminAdPostingResponse>>>
}
