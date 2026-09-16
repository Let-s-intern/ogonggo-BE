package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostRequest
import com.ogonggo.userapi.community.presentation.request.RecruitmentPostListRequest
import com.ogonggo.userapi.community.presentation.request.UpdateRecruitmentPostRequest
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostDetailResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostSummaryResponse
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
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "사이드·스터디 모집")
@RequestMapping("/api/v1/recruitment-posts")
interface RecruitmentPostApi {

    @Operation(
        operationId = "getPublicRecruitmentPost",
        summary = "사이드 프로젝트·스터디 모집글 상세 조회",
        description = """
            공개된 모집글의 기본 정보와 본문을 조회합니다. 로그인 없이 호출할 수 있습니다.

            ### 추가사항

            - 조회 시 조회수 집계 이벤트가 발생합니다.
            - `DRAFT`, `HIDDEN`, 삭제된 글은 조회할 수 없습니다.
            - `CLOSED` 상태의 공개 글은 조회할 수 있습니다.
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
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 모집글을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/{postId}")
    fun getRecruitmentPost(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long?,
        @PathVariable("postId") @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<RecruitmentPostDetailResponse>>

    @Operation(
        summary = "사이드 프로젝트·스터디 모집글 목록 조회",
        description = """
            공개 모집글을 페이지로 페이징하고 모집 구분·진행 방식·모집 상태·포지션으로 필터링합니다.

            ### 추가사항

            - 공개 게시글만 조회됩니다.
            - 비로그인 사용자는 `bookmarked=false`, `bookmarkCount=0`으로 반환됩니다.
            - 다중 필터는 동일한 Query Parameter를 반복해서 전달합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "페이지, 페이지 크기 또는 enum 필터가 올바르지 않음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @GetMapping
    fun getRecruitmentPosts(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long?,
        @ParameterObject @Valid @ModelAttribute request: RecruitmentPostListRequest,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostSummaryResponse>>>

    @Operation(
        summary = "사이드 프로젝트·스터디 모집글 생성",
        description = """
            `saveMode`가 `DRAFT`면 제목 중심으로 임시저장하고, `PUBLISH`면 게시 필수값과 운영 정책 동의를 검증한 뒤 공개합니다.

            ### 추가사항

            - `saveMode` 기본값은 `PUBLISH`입니다. 임시저장 시 반드시 `DRAFT`를 전달해야 합니다.
            - `DRAFT`는 제목만 필수입니다.
            - `PUBLISH`는 전체 게시 필수값과 `agreedToPolicy=true`가 필요합니다.
            - `PUBLISH` 생성 결과의 게시 상태는 `PUBLISHED`, 모집 상태는 `RECRUITING`입니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "요청 필드 검증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping
    fun createRecruitmentPost(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @RequestBody @Valid request: CreateRecruitmentPostRequest,
    ): ResponseEntity<SuccessResponse<CreateRecruitmentPostResponse>>

    @Operation(
        summary = "사이드 프로젝트·스터디 모집글 수정",
        description = """
            `saveMode`가 `PUBLISH`면 임시저장 모집글을 갱신 후 게시 상태로 전환합니다. 생략하면 기존 저장 동작을 따릅니다.

            ### 추가사항

            - 전체 수정 방식이므로 공개 모집글 수정 시 전체 필드를 전달해야 합니다.
            - `saveMode=PUBLISH`이면 임시저장 글을 수정한 뒤 같은 ID로 게시합니다.
            - 모집 마감 글의 기간을 수정해도 자동으로 `RECRUITING` 상태가 되지 않습니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "요청 필드 검증 실패",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 모집글을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "USER_SUSPENDED 또는 USER_WITHDRAWN",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PutMapping("/{postId}")
    fun updateRecruitmentPost(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") @Positive postId: Long,
        @RequestBody @Valid request: UpdateRecruitmentPostRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "closeMyRecruitmentPost",
        summary = "내 사이드 프로젝트·스터디 모집글 마감",
        description = """
            작성자 본인의 모집글을 수동으로 마감합니다.

            ### 추가사항

            - 수동 조기 마감용 API입니다.
            - 이미 마감된 글에 다시 호출해도 성공 처리됩니다.
            - 모집 종료일 다음 날 스케줄러가 자동 마감합니다.
            - 종료일 당일에는 모집 중 상태가 유지됩니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "마감 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "postId가 1 미만",
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
    @PatchMapping("/{postId}/close")
    fun closeRecruitmentPost(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "reopenMyRecruitmentPost",
        summary = "내 사이드 프로젝트·스터디 모집글 재모집",
        description = """
            작성자 본인의 마감된 모집글을 다시 모집 중 상태로 변경합니다.

            ### 추가사항

            - `CLOSED` 상태를 `RECRUITING` 상태로 변경합니다.
            - 모집 기간이나 게시글 내용은 변경하지 않습니다.
            - 이미 모집 중인 글에 호출해도 성공 처리됩니다.
            - 모집 기간 수정만으로 자동 재모집되지는 않습니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "재모집 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "postId가 1 미만",
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
    @PatchMapping("/{postId}/reopen")
    fun reopenRecruitmentPost(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteMyRecruitmentPost",
        summary = "내 사이드 프로젝트·스터디 모집글 삭제",
        description = """
            작성자 본인의 모집글을 소프트 삭제합니다. 이미 삭제된 글을 다시 삭제해도 성공합니다.

            ### 추가사항

            - 실제 데이터 삭제가 아닌 소프트 삭제 방식입니다.
            - 삭제된 글의 첨부 이미지 연결도 해제됩니다.
        """,
    )
    @SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "postId가 1 미만",
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
    @DeleteMapping("/{postId}")
    fun deleteRecruitmentPost(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @PathVariable("postId") @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
