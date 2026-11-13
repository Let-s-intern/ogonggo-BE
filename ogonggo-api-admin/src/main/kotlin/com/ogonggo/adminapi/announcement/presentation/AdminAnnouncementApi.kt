package com.ogonggo.adminapi.announcement.presentation

import io.swagger.v3.oas.annotations.Parameter
import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.announcement.presentation.request.CreateAdminAnnouncementRequest
import com.ogonggo.adminapi.announcement.presentation.request.UpdateAdminAnnouncementRequest
import com.ogonggo.adminapi.announcement.presentation.response.AdminAnnouncementDetailResponse
import com.ogonggo.adminapi.announcement.presentation.response.AdminAnnouncementSummaryResponse
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

private const val ANNOUNCEMENT_NOT_FOUND_DESCRIPTION = "ANNOUNCEMENT_NOT_FOUND: 공지사항을 찾을 수 없습니다."

@Tag(name = "관리자 공지사항")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminAnnouncementApi {

    @Operation(
        operationId = "listAnnouncements",
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
    fun getAnnouncements(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        @Size(max = 100) keyword: String?,
        visibility: AdminContentVisibility?,
        pinned: Boolean?,
        category: AnnouncementCategory?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminAnnouncementSummaryResponse>>>

    @Operation(operationId = "getAnnouncement", summary = "공지사항 상세 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = ANNOUNCEMENT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getAnnouncement(
        @Positive announcementId: Long,
    ): ResponseEntity<SuccessResponse<AdminAnnouncementDetailResponse>>

    @Operation(
        operationId = "createAnnouncement",
        summary = "공지사항 등록",
        description = """
            등록한 공지 전체를 반환합니다. visibility가 VISIBLE이면 곧바로 사용자에게 노출됩니다.
            content는 Lexical EditorState JSON 문자열이며 200,000자 이하여야 합니다.
            pushTarget을 정하고 VISIBLE로 등록하면 그 대상에게 푸시 알림을 한 번 보냅니다. HIDDEN으로 등록하면 나중에 노출할 때 보냅니다.
            푸시 제목은 공지 제목(100자까지), 본문은 요약(없으면 기본 문구)이고 누르면 사용자 공지 상세로 이동합니다.
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
    fun createAnnouncement(
        @Parameter(hidden = true) adminUserId: Long,
        @Valid request: CreateAdminAnnouncementRequest,
    ): ResponseEntity<SuccessResponse<AdminAnnouncementDetailResponse>>

    @Operation(
        operationId = "updateAnnouncement",
        summary = "공지사항 수정",
        description = """
            보낸 값만 바꾸고 수정된 공지 전체를 반환합니다. 제목과 본문은 비울 수 없습니다.
            노출(visibility)과 상단 고정(pinned), 카테고리(category)도 이 API로 바꿉니다.
            썸네일(thumbnailUrl)과 요약(summary)은 빈 글자를 보내면 지웁니다. 상단 고정 공지는 사용자 공지사항 화면의 추천 카드입니다.
            푸시 대상(pushTarget)이 정해진 공지가 이 수정으로 노출 상태가 되면 푸시를 한 번 보냅니다. 노출 중인 공지에 대상을 정해도 바로 보냅니다.
            NONE이면 노출해도 보내지 않고, 이미 보낸 공지는 숨겼다 다시 노출해도 다시 보내지 않습니다.
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
                responseCode = "409",
                description = "ANNOUNCEMENT_PUSH_ALREADY_SENT: 이미 푸시를 보낸 공지의 푸시 대상을 바꾸려 했습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = ANNOUNCEMENT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun updateAnnouncement(
        @Parameter(hidden = true) adminUserId: Long,
        @Positive announcementId: Long,
        @Valid request: UpdateAdminAnnouncementRequest,
    ): ResponseEntity<SuccessResponse<AdminAnnouncementDetailResponse>>

    @Operation(
        operationId = "deleteAnnouncement",
        summary = "공지사항 삭제",
        description = "소프트 삭제합니다. 이미 삭제한 공지를 다시 삭제해도 성공하며 최초 삭제 일시를 유지합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = ANNOUNCEMENT_NOT_FOUND_DESCRIPTION,
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteAnnouncement(
        @Positive announcementId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
