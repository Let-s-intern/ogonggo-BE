package com.ogonggo.userapi.announcement.presentation

import com.ogonggo.userapi.announcement.presentation.response.UserAnnouncementDetailResponse
import com.ogonggo.userapi.announcement.presentation.response.UserAnnouncementSummaryResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity

@Tag(name = "공지사항")
interface UserAnnouncementApi {

    @Operation(
        operationId = "listPublicAnnouncements",
        summary = "공지사항 목록 조회",
        description = """
            로그인 없이 조회할 수 있습니다. 노출 중인 공지만 반환하며 목록에는 본문을 싣지 않습니다.
            상단 고정 공지가 먼저 오고 그 안에서 최신순입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 페이지 범위가 올바르지 않습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getAnnouncements(
        @Min(1)
        page: Int,
        @Min(1)
        @Max(100)
        size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<UserAnnouncementSummaryResponse>>>

    @Operation(
        operationId = "getPublicAnnouncement",
        summary = "공지사항 상세 조회",
        description = "로그인 없이 조회할 수 있습니다. 비노출이거나 삭제된 공지는 404입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "ANNOUNCEMENT_NOT_FOUND: 공지사항을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getAnnouncement(
        @Positive
        announcementId: Long,
    ): ResponseEntity<SuccessResponse<UserAnnouncementDetailResponse>>
}
