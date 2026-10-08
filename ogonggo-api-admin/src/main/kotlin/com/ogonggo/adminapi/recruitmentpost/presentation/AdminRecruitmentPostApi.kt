package com.ogonggo.adminapi.recruitmentpost.presentation

import com.ogonggo.adminapi.recruitmentpost.presentation.request.ChangeAdminRecruitmentPostVisibilityRequest
import com.ogonggo.adminapi.recruitmentpost.presentation.response.AdminRecruitmentPostSummaryResponse
import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.content.business.AdminContentSortType
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
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
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity

@Tag(name = "관리자 사이드·스터디")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminRecruitmentPostApi {

    @Operation(
        operationId = "listRecruitmentPosts",
        summary = "사이드·스터디 모집글 목록 조회",
        description = """
            노출 여부와 무관하게 삭제되지 않은 모집글을 반환합니다. 작성자만 보는 임시저장 글은 싣지 않습니다.
            정렬은 채용공고·부트캠프 목록과 같습니다.

            keyword는 제목에서 대소문자를 가리지 않고 부분 일치로 찾습니다.
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
    fun getRecruitmentPosts(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        sortType: AdminContentSortType,
        @Size(max = 100) keyword: String?,
        visibility: AdminContentVisibility?,
        recruitmentType: RecruitmentPostType?,
        recruitmentStatus: RecruitmentPostRecruitmentStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminRecruitmentPostSummaryResponse>>>

    @Operation(
        operationId = "updateRecruitmentPostVisibilities",
        summary = "사이드·스터디 모집글 노출 일괄 변경",
        description = """
            ids의 모집글을 모두 visibility로 바꿉니다. 규칙은 채용공고 노출 일괄 변경과 같습니다.
            한 번에 1~1000건을 보낼 수 있고, 하나라도 바꿀 수 없으면 아무것도 바꾸지 않습니다.
            운영자가 숨긴 모집글은 작성자가 다시 게시할 수 없고, 운영자가 VISIBLE로 바꿔야 다시 나옵니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: ids가 비었거나 1000건을 넘거나 양수가 아닌 값이 있거나 visibility가 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "RECRUITMENT_POST_NOT_FOUND: 없거나 삭제됐거나 임시저장인 모집글이 들어 있습니다. " +
                    "예: 모집글을 찾을 수 없습니다. (id: 7, 999)",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun changeRecruitmentPostVisibilities(
        @Valid request: ChangeAdminRecruitmentPostVisibilityRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
