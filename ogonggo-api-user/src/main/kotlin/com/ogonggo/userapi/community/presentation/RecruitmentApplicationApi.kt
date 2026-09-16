package com.ogonggo.userapi.community.presentation

import com.ogonggo.core.community.domain.RecruitmentApplicationSortType
import com.ogonggo.core.community.domain.RecruitmentApplicationProgressStatus
import com.ogonggo.userapi.community.presentation.request.UpdateRecruitmentApplicationStatusRequest
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostApplicationResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentApplicationPageResponse
import com.ogonggo.userapi.config.USER_BEARER_AUTH_SCHEME
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
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "모집글 지원")
@SecurityRequirement(name = USER_BEARER_AUTH_SCHEME)
interface RecruitmentApplicationApi {

    @Operation(
        operationId = "createRecruitmentPostApplication",
        summary = "모집글 외부 지원 링크 접근 기록",
        description = """
            실제 지원서 제출이 아니라 모집글의 외부 지원 연락처를 열었다는 이력을 저장합니다.

            ### 추가사항

            - 성공 후 FE가 응답의 `contactValue`를 사용해 카카오톡 또는 이메일을 열어야 합니다.
            - 동일 사용자의 재접근은 새 이력을 생성하지 않고 `lastClickedAt`만 갱신합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "기록 성공", useReturnTypeSchema = true),
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
                description = "RECRUITMENT_POST_NOT_FOUND: 게시된 모집글을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "RECRUITMENT_POST_CLOSED: 마감된 모집글에는 지원할 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PostMapping("/api/v1/recruitment-posts/{postId}/applications")
    fun createApplication(
        @Parameter(hidden = true) userId: Long,
        @PathVariable("postId") @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<CreateRecruitmentPostApplicationResponse>>

    @Operation(
        operationId = "listMyRecruitmentApplications",
        summary = "내 모집글 지원 이력 목록 조회",
        description = """
            모집글의 외부 지원 링크를 연 이력을 최초 저장 시각 기준 최근 저장순으로 조회합니다. 지원 상태는 사용자의 개인 관리 상태입니다.

            keyword를 보내면 모집글 제목에 포함되는지 대소문자를 구분하지 않고 검색합니다.
            검색어는 2자 이상 100자 이하여야 하며, 모집 상태·모집 유형·지원 상태 필터와 함께 사용할 수 있습니다.
            countsByRecruitmentType에는 현재 검색·모집 상태·지원 상태 필터를 적용한 SIDE_PROJECT, STUDY 건수를 반환합니다.

            ### 추가사항

            - `applicationStatus`는 사용자의 개인 관리 상태입니다.
            - 지원 이력은 최초 접근 시각 기준으로 정렬됩니다.
            - `countsByRecruitmentType`는 현재 필터 조건이 적용된 결과입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "페이지·정렬·필터 파라미터가 올바르지 않음",
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
    @GetMapping("/api/v1/me/recruitment-applications")
    fun getApplications(
        @Parameter(hidden = true) userId: Long,
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        recruitmentStatus: RecruitmentStatus?,
        recruitmentType: RecruitmentType?,
        @Size(min = 2, max = 100) keyword: String?,
        sort: RecruitmentApplicationSortType,
        applicationStatus: RecruitmentApplicationProgressStatus?,
    ): ResponseEntity<SuccessResponse<RecruitmentApplicationPageResponse>>

    @Operation(
        operationId = "updateRecruitmentPostApplicationStatus",
        summary = "내 모집글 지원 상태 변경",
        description = """
            실제 지원서 처리 상태가 아닌 사용자의 개인 관리 상태를 변경합니다.

            ### 추가사항

            - 모집글 작성자에게 보이는 지원자 정보에는 영향을 주지 않습니다.
            - 삭제된 지원 이력은 상태를 변경할 수 없습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "applicationStatus 또는 postId가 올바르지 않음",
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
                description = "RECRUITMENT_POST_APPLICATION_NOT_FOUND: 지원 이력을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @PatchMapping("/api/v1/me/recruitment-applications/{postId}")
    fun updateApplicationStatus(
        @Parameter(hidden = true) userId: Long,
        @PathVariable("postId") @Positive postId: Long,
        @RequestBody @Valid request: UpdateRecruitmentApplicationStatusRequest,
    ): ResponseEntity<SuccessResponse<Unit>>

    @Operation(
        operationId = "deleteRecruitmentPostApplication",
        summary = "내 모집글 지원 이력 삭제",
        description = """
            지원 이력을 개인 목록에서 숨기고 모집글의 활성 `applicationCount` 집계에서도 제외합니다.

            ### 추가사항

            - 지원 이력은 소프트 삭제됩니다.
            - 이후 다시 외부 지원 링크에 접근하면 기존 이력이 재활성화됩니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
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
                description = "RECRUITMENT_POST_APPLICATION_NOT_FOUND: 지원 이력을 찾을 수 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @DeleteMapping("/api/v1/me/recruitment-applications/{postId}")
    fun deleteApplication(
        @Parameter(hidden = true) userId: Long,
        @PathVariable("postId") @Positive postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>>
}
