package com.ogonggo.adminapi.bootcamp.presentation

import com.ogonggo.adminapi.bootcamp.presentation.request.UpdateAdminBootcampRequest
import com.ogonggo.adminapi.bootcamp.presentation.response.AdminBootcampDetailResponse
import com.ogonggo.adminapi.bootcamp.presentation.response.AdminBootcampSummaryResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.content.business.AdminContentSortType
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

private const val BOOTCAMP_NOT_FOUND_DESCRIPTION = "BOOTCAMP_NOT_FOUND: 부트캠프를 찾을 수 없습니다."

@Tag(name = "관리자 부트캠프")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminBootcampApi {

    @Operation(
        operationId = "listBootcamps",
        summary = "부트캠프 목록 조회",
        description = """
            공개 여부와 무관하게 삭제되지 않은 부트캠프를 반환합니다. 필터와 정렬은 채용공고 목록과 같습니다.

            keyword는 과정명과 운영사에서 대소문자를 가리지 않고 부분 일치로 찾습니다.
            status는 모집 상태이며 RECRUITING과 CLOSED만 받습니다. DRAFT를 보내면 400입니다.
            필터는 모두 AND로 묶이고 값을 보내지 않거나 빈 값을 보내면 그 조건을 적용하지 않습니다.
            마지막 페이지를 넘는 page는 빈 items를 반환합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 페이지 범위나 필터 값이 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 액세스 토큰이 없거나 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "FORBIDDEN: 활성 상태의 관리자 계정이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getBootcamps(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        sortType: AdminContentSortType,
        @Size(max = 100) keyword: String?,
        visibility: AdminContentVisibility?,
        source: ContentSource?,
        reviewStatus: ReviewStatus?,
        status: BootcampStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminBootcampSummaryResponse>>>

    @Operation(operationId = "getBootcamp", summary = "부트캠프 상세 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = BOOTCAMP_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getBootcamp(
        @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<AdminBootcampDetailResponse>>

    @Operation(
        operationId = "updateBootcamp",
        summary = "부트캠프 운영 값·내용 수정",
        description = """
            보낸 값만 바꾸고 수정된 부트캠프 전체를 반환합니다. 규칙은 채용공고 수정과 같습니다.

            fields는 content, eligibilityAndSelectionProcess만 반영하고 나머지 키는 버립니다.
            빈 문자열은 그 칸을 비우지만 content는 비울 수 없어 400입니다.
            커리큘럼과 파트너사는 이 API로 고치지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 과정명이나 상세 내용을 비웠거나 reviewStatus에 REJECTED를 보냈습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = BOOTCAMP_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "REVIEW_NOT_APPROVED 또는 CONTENT_NOT_REVIEWABLE",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateBootcamp(
        @Positive bootcampId: Long,
        @Valid request: UpdateAdminBootcampRequest,
    ): ResponseEntity<SuccessResponse<AdminBootcampDetailResponse>>

    @Operation(
        operationId = "deleteBootcamp",
        summary = "부트캠프 삭제",
        description = """
            소프트 삭제합니다. 이미 삭제한 부트캠프를 다시 삭제해도 성공하며 최초 삭제 일시를 유지합니다.
            반려 기록은 지우지 않고 반려 보관 목록에 contentExists=false로 남깁니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = BOOTCAMP_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteBootcamp(
        @Positive bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
