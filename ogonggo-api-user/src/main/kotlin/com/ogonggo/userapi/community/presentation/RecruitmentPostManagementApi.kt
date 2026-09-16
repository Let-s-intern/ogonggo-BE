package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.community.domain.RecruitmentPostApplicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.community.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostFormResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostManagementItemResponse
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostResponse
import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostDraftRequest
import com.ogonggo.userapi.community.presentation.request.PublishRecruitmentPostRequest
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
import jakarta.validation.constraints.Size
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "사이드·스터디 모집 관리")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface RecruitmentPostManagementApi {

    @Operation(
        operationId = "createMyRecruitmentPostDraft",
        summary = "내 모집글 임시저장 생성",
        description = """
            제목만 필수로 받고 나머지 필드는 선택적으로 저장합니다.

            ### 추가사항

            - 기존 호환용 임시저장 API입니다.
            - 신규 생성 화면에서는 `POST /api/v1/recruitment-posts`에 `saveMode=DRAFT`를 사용하는 것을 권장합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "제목 또는 입력된 필드 검증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/drafts")
    fun createMyRecruitmentPostDraft(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: CreateRecruitmentPostDraftRequest,
    ): ResponseEntity<SuccessResponse<CreateRecruitmentPostResponse>>

    @Operation(
        operationId = "copyMyRecruitmentPost",
        summary = "내 모집글 복사",
        description = """
            작성자의 모집글을 새 임시저장 모집글로 복사하고 작성 화면용 데이터를 반환합니다.

            ### 추가사항

            - 새로운 `postId`를 가진 `DRAFT` 글이 생성됩니다.
            - 본문과 이미지 정보가 복사됩니다.
            - `agreedToPolicy`는 `false`입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "복사 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "postId가 1 미만",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 모집글이 없거나 본인 글이 아님",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/{postId}/copies")
    fun copyMyRecruitmentPost(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") @Min(1) postId: Long,
    ): ResponseEntity<SuccessResponse<RecruitmentPostFormResponse>>

    @Operation(
        operationId = "publishMyRecruitmentPost",
        summary = "내 모집글 게시",
        description = """
            임시저장 모집글의 필수값과 정책 동의를 검증한 뒤 공개 상태로 전환합니다. 이미 게시된 글은 멱등 성공합니다.

            ### 추가사항

            - 기존 호환용 게시 API입니다.
            - 신규 수정·게시 흐름에서는 `PUT /api/v1/recruitment-posts/{postId}`와 `saveMode=PUBLISH` 사용을 권장합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "게시 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "게시 필수값 또는 정책 동의 검증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 모집글이 없거나 본인 글이 아님",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/{postId}/publish")
    fun publishMyRecruitmentPost(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") @Min(1) postId: Long,
        @RequestBody @Valid request: PublishRecruitmentPostRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "getMyRecruitmentPostForm",
        summary = "내 모집글 작성 폼 상세 조회",
        description = """
            작성자의 임시저장·공개·비공개 모집글을 작성 화면용 전체 필드로 조회합니다.

            ### 추가사항

            - 본인이 작성한 `DRAFT`, `PUBLISHED`, `HIDDEN` 글을 조회할 수 있습니다.
            - `content`는 문자열이 아닌 JSON 객체입니다.
            - `agreedToPolicy`는 현재 항상 `false`로 반환됩니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "postId가 1 미만",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 모집글이 없거나 본인 글이 아님",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/{postId}")
    fun getMyRecruitmentPostForm(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") @Min(1) postId: Long,
    ): ResponseEntity<SuccessResponse<RecruitmentPostFormResponse>>

    @Operation(
        operationId = "listMyRecruitmentPosts",
        summary = "내 사이드 프로젝트·스터디 모집글 관리 목록 조회",
        description = """
            임시저장·공개·비공개 모집글을 최근 저장순으로 조회합니다.

            keyword를 보내면 모집글 제목에 포함되는지 대소문자를 구분하지 않고 검색합니다.
            검색어는 2자 이상 100자 이하여야 하며, 게시 상태·모집 상태·지원 이력·모집 유형 필터와 함께 사용할 수 있습니다.

            ### 추가사항

            - `keyword`는 앞뒤 공백을 제거한 뒤 검색합니다.
            - `DRAFT` 글의 `recruitmentStatus`는 `null`입니다.
            - `DRAFT` 글의 `continueWriting`은 `true`입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "페이지·필터·검색 파라미터가 올바르지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "UNAUTHORIZED: 인증이 필요합니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getMyRecruitmentPosts(
        @Parameter(hidden = true) userId: Long,
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        status: RecruitmentPostManagementStatus,
        recruitmentStatus: RecruitmentStatus?,
        applicationStatus: RecruitmentPostApplicationStatus?,
        recruitmentType: RecruitmentType?,
        @Size(min = 2, max = 100) keyword: String?,
        sort: RecruitmentPostManagementSortType,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostManagementItemResponse>>>
}
