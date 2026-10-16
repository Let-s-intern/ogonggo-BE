package com.ogonggo.adminapi.member.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.member.presentation.request.ChangeGeneralMemberRoleRequest
import com.ogonggo.adminapi.member.presentation.response.AdminGeneralMemberResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.user.domain.UserInflowSource
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
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
import org.springframework.security.core.annotation.AuthenticationPrincipal
import java.time.LocalDate

@Tag(name = "관리자 일반 회원")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminGeneralMemberApi {

    @Operation(
        operationId = "listGeneralMembers",
        summary = "일반 회원 목록 조회",
        description = """
            렛츠커리어로 가입한 일반 회원을 최근 가입 순으로 반환합니다. 멘토(MENTOR)와 관리자(ADMIN)도 나오며 기업 회원은 나오지 않습니다.
            탈퇴·정지한 회원도 포함하며 status로 거를 수 있습니다. role로 USER·MENTOR·ADMIN 중 하나만 거를 수 있습니다.
            inflowSource로 렛츠커리어 기존 회원(LETSCAREER)과 오공고 신규 유입(OGONGGO)을 거를 수 있습니다.

            keyword는 닉네임이나 이메일에서 대소문자를 가리지 않고 부분 일치로 찾습니다.
            joinedFrom·joinedTo는 가입일(YYYY-MM-DD) 범위이며 두 날짜를 모두 포함합니다.
            필터는 모두 AND로 묶이고 값을 보내지 않거나 빈 값을 보내면 그 조건을 적용하지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 페이지 범위나 필터 값이 올바르지 않거나, 가입 기간 시작일이 종료일보다 늦거나, role이 COMPANY입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getGeneralMembers(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        @Parameter(description = "닉네임이나 이메일에서 찾을 검색어") @Size(max = 100) keyword: String?,
        status: UserStatus?,
        @Parameter(description = "가입일 시작(포함)") joinedFrom: LocalDate?,
        @Parameter(description = "가입일 끝(포함)") joinedTo: LocalDate?,
        @Parameter(description = "역할. USER·MENTOR·ADMIN만 받습니다.") role: UserRole?,
        @Parameter(description = "유입 구분. 아직 정해지지 않은 회원은 어느 값으로 걸러도 나오지 않습니다.") inflowSource: UserInflowSource?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminGeneralMemberResponse>>>

    @Operation(
        operationId = "changeGeneralMemberRole",
        summary = "일반 회원 역할 변경",
        description = """
            렛츠커리어로 가입한 일반 회원의 역할을 USER·MENTOR·ADMIN 중 하나로 바꿉니다. 올리기와 내리기를 모두 이 API로 합니다.
            지금과 같은 역할을 보내면 아무것도 바꾸지 않고 성공합니다. 변경 이력은 남기지 않습니다.
            정지한 회원은 바꿀 수 있고, 기업 회원·탈퇴 회원과 자기 자신은 바꿀 수 없습니다.
            관리자 권한은 요청마다 DB의 역할로 확인하므로 ADMIN으로 바꾸거나 ADMIN에서 내린 결과가 그 회원의 다음 요청부터 적용됩니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: role이 없거나 COMPANY입니다. " +
                    "COMPANY_MEMBER_ROLE_UNCHANGEABLE: 기업 회원입니다. " +
                    "OWN_ROLE_UNCHANGEABLE: 자기 역할을 바꾸려 했습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "USER_NOT_FOUND: 회원이 없습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "WITHDRAWN_USER_ROLE_UNCHANGEABLE: 탈퇴한 회원입니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun changeGeneralMemberRole(
        @AuthenticationPrincipal adminUserId: Long,
        @Positive userId: Long,
        @Valid request: ChangeGeneralMemberRoleRequest,
    ): ResponseEntity<SuccessResponse<Unit>>
}
