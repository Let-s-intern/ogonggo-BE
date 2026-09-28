package com.ogonggo.adminapi.notice.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.notice.presentation.request.CreateAdminNoticeRequest
import com.ogonggo.adminapi.notice.presentation.request.UpdateAdminNoticeRequest
import com.ogonggo.adminapi.notice.presentation.response.AdminNoticeDetailResponse
import com.ogonggo.adminapi.notice.presentation.response.AdminNoticeSummaryResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
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

private const val NOTICE_NOT_FOUND_DESCRIPTION = "NOTICE_NOT_FOUND: 공지사항을 찾을 수 없습니다."

@Tag(name = "관리자 공지사항")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminNoticeApi {

    @Operation(
        operationId = "listNotices",
        summary = "공지사항 목록 조회",
        description = """
            노출 여부와 무관하게 삭제되지 않은 공지를 반환합니다. 상단 고정 공지가 먼저 오고 그 안에서 최신순입니다.

            keyword는 제목에서 대소문자를 가리지 않고 부분 일치로 찾습니다.
            필터는 모두 AND로 묶이고 값을 보내지 않거나 빈 값을 보내면 그 조건을 적용하지 않습니다.
            목록에는 본문을 싣지 않습니다.
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
        ],
    )
    fun getNotices(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        @Size(max = 100) keyword: String?,
        visibility: AdminContentVisibility?,
        pinned: Boolean?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminNoticeSummaryResponse>>>

    @Operation(operationId = "getNotice", summary = "공지사항 상세 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = NOTICE_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getNotice(
        @Positive noticeId: Long,
    ): ResponseEntity<SuccessResponse<AdminNoticeDetailResponse>>

    @Operation(
        operationId = "createNotice",
        summary = "공지사항 등록",
        description = """
            등록한 공지 전체를 반환합니다. visibility가 VISIBLE이면 곧바로 사용자에게 노출됩니다.
            content는 Lexical EditorState JSON 문자열이며 200,000자 이하여야 합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 필수 값이 없거나 본문이 올바른 JSON이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createNotice(
        @Valid request: CreateAdminNoticeRequest,
    ): ResponseEntity<SuccessResponse<AdminNoticeDetailResponse>>

    @Operation(
        operationId = "updateNotice",
        summary = "공지사항 수정",
        description = """
            보낸 값만 바꾸고 수정된 공지 전체를 반환합니다. 제목과 본문은 비울 수 없습니다.
            노출(visibility)과 상단 고정(pinned)도 이 API로 바꿉니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 제목을 비웠거나 본문이 올바른 JSON이 아닙니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = NOTICE_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateNotice(
        @Positive noticeId: Long,
        @Valid request: UpdateAdminNoticeRequest,
    ): ResponseEntity<SuccessResponse<AdminNoticeDetailResponse>>

    @Operation(
        operationId = "deleteNotice",
        summary = "공지사항 삭제",
        description = "소프트 삭제합니다. 이미 삭제한 공지를 다시 삭제해도 성공하며 최초 삭제 일시를 유지합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = NOTICE_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteNotice(
        @Positive noticeId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
