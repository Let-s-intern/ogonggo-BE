package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bookmark.domain.ApplicationStatus
import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.userapi.bootcamp.presentation.response.UserBootcampSummaryResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.ErrorResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

@Tag(name = "부트캠프 북마크")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface UserBootcampBookmarkApi {

    @Operation(
        operationId = "listMyBootcampBookmarks",
        summary = "부트캠프 북마크 목록 조회",
        description = """
            북마크한 부트캠프 중 지금 공개된 부트캠프만 반환합니다.

            부트캠프 목록과 같은 필터를 사용할 수 있습니다.
            tuitionType과 status로 목록을 좁히며 각각 하나씩 고를 수 있고, 보내지 않으면 해당 조건을 적용하지 않습니다.
            status는 RECRUITING(모집중)과 CLOSED(모집 마감)만 받으며 그 밖의 값은 400입니다.

            keyword는 운영 회사명 또는 프로그램명에 포함되는지로 찾으며 대소문자를 가리지 않습니다.
            2자 이상 100자 이하여야 하며, 검색하지 않을 때는 보내지 않습니다.

            applicationStatus는 지원·신청 관리 단계로 SCRAPPED(스크랩), PREPARING(지원 준비 중), APPLIED(지원 완료),
            INTERVIEWING(면접), PASSED(합격), FAILED(불합격) 중 하나입니다.
            보내면 그 단계의 북마크만 반환하고, 보내지 않으면 모든 단계를 반환합니다.

            sort로 정렬을 고릅니다. 지금은 RECENTLY_SAVED(최근 저장순)만 있으며 보내지 않으면 RECENTLY_SAVED입니다.
            북마크를 등록·재등록하거나 지원 단계를 옮긴 시각이 최근인 순서입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 공개 목록에서 고를 수 없는 모집 상태입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getBookmarks(
        @Parameter(hidden = true)
        userId: Long,
        @Min(1)
        page: Int,
        @Min(1)
        @Max(100)
        size: Int,
        sortType: BookmarkSortType,
        tuitionType: TuitionType?,
        status: BootcampStatus?,
        @Size(min = 2, max = 100)
        keyword: String?,
        applicationStatus: ApplicationStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<UserBootcampSummaryResponse>>>

    @Operation(operationId = "createBootcampBookmark", summary = "부트캠프 북마크 등록")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "BOOTCAMP_NOT_FOUND: 부트캠프를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "BOOTCAMP_BOOKMARK_ALREADY_EXISTS: 이미 북마크한 부트캠프입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun addBookmark(
        @Parameter(hidden = true)
        userId: Long,
        @Positive
        bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(operationId = "deleteBootcampBookmark", summary = "부트캠프 북마크 해제")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "해제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "BOOTCAMP_NOT_FOUND: 부트캠프를 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteBookmark(
        @Parameter(hidden = true)
        userId: Long,
        @Positive
        bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "prepareMyBootcampBookmark",
        summary = "부트캠프 북마크를 지원 준비 중으로 이동",
        description = """
            스크랩 단계의 북마크를 지원 준비 중으로 옮깁니다.
            이미 지원 준비 중이면 아무것도 바꾸지 않고 200으로 응답합니다.
            옮긴 북마크는 해당 단계 목록의 맨 앞에 옵니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "이동 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "BOOTCAMP_BOOKMARK_NOT_FOUND: 북마크하지 않은 부트캠프입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "INVALID_BOOTCAMP_APPLICATION_STATUS_TRANSITION: 허용되지 않는 신청 단계 변경입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun prepare(
        @Parameter(hidden = true)
        userId: Long,
        @Positive
        bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "cancelMyBootcampBookmarkPreparation",
        summary = "부트캠프 북마크를 스크랩으로 되돌리기",
        description = """
            지원 준비 중인 북마크를 스크랩 단계로 되돌립니다.
            이미 스크랩 단계면 아무것도 바꾸지 않고 200으로 응답합니다.
            옮긴 북마크는 해당 단계 목록의 맨 앞에 옵니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "이동 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "BOOTCAMP_BOOKMARK_NOT_FOUND: 북마크하지 않은 부트캠프입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "INVALID_BOOTCAMP_APPLICATION_STATUS_TRANSITION: 허용되지 않는 신청 단계 변경입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun cancelPreparation(
        @Parameter(hidden = true)
        userId: Long,
        @Positive
        bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
