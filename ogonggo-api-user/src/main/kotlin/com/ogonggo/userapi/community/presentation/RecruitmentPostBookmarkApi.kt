package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.bookmark.domain.BookmarkSortType
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostSummaryResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.ErrorResponse
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

@Tag(name = "사이드·스터디 모집글 북마크")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface RecruitmentPostBookmarkApi {

    @Operation(
        operationId = "listMyRecruitmentPostBookmarks",
        summary = "사이드·스터디 모집글 북마크 목록 조회",
        description = """
            북마크 목록을 조회합니다. 마이페이지 지원·신청 관리의 스크랩 칸에도 이 목록을 씁니다.
            지원 준비 중 이후 칸은 `GET /api/v1/me/recruitment-applications`의 지원 이력을 씁니다.

            recruitmentStatus(RECRUITING 모집 중, CLOSED 마감), recruitmentType(SIDE_PROJECT, STUDY)으로 좁히며
            보내지 않으면 해당 조건을 적용하지 않습니다.
            keyword는 모집글 제목에 포함되는지로 찾으며 대소문자를 가리지 않고, 2자 이상 100자 이하여야 합니다.
            sort는 지금 RECENTLY_SAVED(최근 저장순)만 있으며 북마크를 마지막으로 활성화한 순서입니다.

            ### 추가사항

            - 현재 활성화된 북마크 중 공개 글만 반환합니다.
            - 응답 아이템의 `bookmarked`는 항상 `true`입니다.
        """,
    )
    fun getBookmarks(
        @Parameter(hidden = true) userId: Long,
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        recruitmentStatus: RecruitmentStatus?,
        recruitmentType: RecruitmentType?,
        @Size(min = 2, max = 100) keyword: String?,
        sortType: BookmarkSortType,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostSummaryResponse>>>

    @Operation(
        operationId = "createRecruitmentPostBookmark",
        summary = "사이드·스터디 모집글 북마크 등록",
        description = """
            사이드 프로젝트·스터디 모집글을 북마크합니다.

            ### 추가사항

            - `CLOSED` 상태의 공개 모집글도 북마크할 수 있습니다.
            - 삭제된 모집글은 북마크할 수 없습니다.
            - 기존 비활성 북마크가 있으면 재활성화됩니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 모집글을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS: 이미 북마크한 모집글입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun addBookmark(
        @Parameter(hidden = true) userId: Long,
        @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteRecruitmentPostBookmark",
        summary = "사이드·스터디 모집글 북마크 해제",
        description = """
            사이드 프로젝트·스터디 모집글의 북마크를 해제합니다.

            ### 추가사항

            - 삭제된 모집글도 북마크 해제가 가능합니다.
            - 북마크가 없는 상태에서 호출해도 멱등적으로 처리됩니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "해제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 모집글을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun deleteBookmark(
        @Parameter(hidden = true) userId: Long,
        @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "prepareMyRecruitmentPostBookmark",
        summary = "사이드·스터디 북마크를 지원 준비 중으로 이동",
        description = """
            스크랩 칸의 모집글을 지원 준비 중 칸으로 옮깁니다.
            북마크를 해제하고 지원 준비 중(PREPARING) 지원 이력을 만듭니다. 외부 연락처를 열지 않아도 됩니다.

            ### 추가사항

            - 이미 지원 준비 중 이력이 있으면 북마크만 해제하고 200으로 응답합니다.
            - 지원 완료·활동 중·활동 완료 이력이 있으면 409입니다.
            - 지운 지원 이력이 있으면 새로 만들지 않고 지원 준비 중으로 되살립니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "이동 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND 또는 RECRUITMENT_POST_BOOKMARK_NOT_FOUND: 모집글이나 북마크가 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "INVALID_RECRUITMENT_APPLICATION_STATUS_TRANSITION: 허용되지 않는 지원 단계 변경입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun prepare(
        @Parameter(hidden = true) userId: Long,
        @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "cancelMyRecruitmentPostBookmarkPreparation",
        summary = "사이드·스터디 지원 준비 중을 스크랩으로 되돌리기",
        description = """
            지원 준비 중 칸의 모집글을 스크랩 칸으로 되돌립니다.
            지원 이력을 지우고, 북마크가 없으면 다시 북마크합니다.

            ### 추가사항

            - 지원 이력이 없고 북마크만 있으면 아무것도 바꾸지 않고 200으로 응답합니다.
            - 지원 완료·활동 중·활동 완료 이력이 있으면 409입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "이동 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND 또는 RECRUITMENT_POST_BOOKMARK_NOT_FOUND: 모집글이나 북마크가 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "INVALID_RECRUITMENT_APPLICATION_STATUS_TRANSITION: 허용되지 않는 지원 단계 변경입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun cancelPreparation(
        @Parameter(hidden = true) userId: Long,
        @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
