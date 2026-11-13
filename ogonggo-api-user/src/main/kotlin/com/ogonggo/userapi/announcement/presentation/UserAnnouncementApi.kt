package com.ogonggo.userapi.announcement.presentation

import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.userapi.announcement.presentation.response.UserAnnouncementCategoryResponse
import com.ogonggo.userapi.announcement.presentation.response.UserAnnouncementDetailResponse
import com.ogonggo.userapi.announcement.presentation.response.UserAnnouncementSummaryResponse
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

@Tag(name = "공지사항")
interface UserAnnouncementApi {

    @Operation(
        operationId = "listPublicAnnouncements",
        summary = "공지사항 목록 조회",
        description = """
            로그인 없이 조회할 수 있습니다. 노출 중인 공지만 반환하며 목록에는 본문을 싣지 않습니다.
            상단 고정 공지가 먼저 오고 그 안에서 최신순입니다.
            카테고리(`category`), 고정 여부(`pinned`), 검색어(`keyword`)로 거를 수 있습니다. 검색어는 제목·요약·본문에서
            부분 일치로 찾습니다. 공지사항 화면의 맨 위 추천 카드는 `pinned=true` 로 부릅니다.
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
        category: AnnouncementCategory?,
        @Parameter(description = "true면 상단 고정 공지만, false면 고정하지 않은 공지만 줍니다. 보내지 않으면 모두입니다.")
        pinned: Boolean?,
        @Parameter(description = "제목·요약·본문 검색어")
        @Size(min = 1, max = 100)
        keyword: String?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserAnnouncementSummaryResponse>>>

    @Operation(
        operationId = "listPublicAnnouncementCategories",
        summary = "공지사항 카테고리 목록 조회",
        description = """
            로그인 없이 조회할 수 있습니다. 공지사항 화면의 `전체 카테고리` 칸들입니다.
            모든 카테고리를 정해진 순서대로 주며, 카테고리마다 노출 중인 공지 수와 최근 공지 3건을 싣습니다.
            공지가 없는 카테고리도 0건과 빈 목록으로 줍니다.
        """,
    )
    fun getAnnouncementCategories(): ResponseEntity<SuccessResponse<List<UserAnnouncementCategoryResponse>>>

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
