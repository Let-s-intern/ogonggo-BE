package com.ogonggo.adminapi.member.presentation

import com.ogonggo.adminapi.config.ADMIN_BEARER_AUTH_SCHEME
import com.ogonggo.adminapi.member.presentation.response.AdminCompanyMemberResponse
import com.ogonggo.adminapi.response.ErrorResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.user.domain.UserStatus
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
import org.springframework.http.ResponseEntity
import java.time.LocalDate

@Tag(name = "관리자 비즈니스 회원")
@SecurityRequirement(name = ADMIN_BEARER_AUTH_SCHEME)
interface AdminCompanyMemberApi {

    @Operation(
        operationId = "listCompanyMembers",
        summary = "비즈니스 회원 목록 조회",
        description = """
            기업용 회원가입으로 만든 비즈니스(기업) 회원을 최근 가입 순으로 반환합니다.
            탈퇴·정지한 회원도 포함하며 status로 거를 수 있습니다.

            keyword는 회사명이나 담당자 이름에서 대소문자를 가리지 않고 부분 일치로 찾습니다.
            joinedFrom·joinedTo는 가입일(YYYY-MM-DD) 범위이며 두 날짜를 모두 포함합니다.
            필터는 모두 AND로 묶이고 값을 보내지 않거나 빈 값을 보내면 그 조건을 적용하지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            ApiResponse(
                responseCode = "400",
                description = "BAD_REQUEST: 페이지 범위나 필터 값이 올바르지 않거나 가입 기간 시작일이 종료일보다 늦습니다.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun getCompanyMembers(
        @Min(1) page: Int,
        @Min(1) @Max(100) size: Int,
        @Parameter(description = "회사명이나 담당자 이름에서 찾을 검색어") @Size(max = 100) keyword: String?,
        status: UserStatus?,
        @Parameter(description = "가입일 시작(포함)") joinedFrom: LocalDate?,
        @Parameter(description = "가입일 끝(포함)") joinedTo: LocalDate?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminCompanyMemberResponse>>>
}
