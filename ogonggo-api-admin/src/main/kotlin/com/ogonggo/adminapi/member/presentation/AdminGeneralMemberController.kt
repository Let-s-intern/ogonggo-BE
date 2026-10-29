package com.ogonggo.adminapi.member.presentation

import com.ogonggo.adminapi.member.business.AdminMemberService
import com.ogonggo.adminapi.member.presentation.request.ChangeGeneralMemberRoleRequest
import com.ogonggo.adminapi.member.presentation.response.AdminGeneralMemberDetailResponse
import com.ogonggo.adminapi.member.presentation.response.AdminGeneralMemberResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.user.domain.UserInflowSource
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/api/v1/admin/general-members")
class AdminGeneralMemberController(
    private val adminMemberService: AdminMemberService,
) : AdminGeneralMemberApi {

    @GetMapping
    override fun getGeneralMembers(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "status", required = false) status: UserStatus?,
        @RequestParam(name = "joinedFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        joinedFrom: LocalDate?,
        @RequestParam(name = "joinedTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        joinedTo: LocalDate?,
        @RequestParam(name = "role", required = false) role: UserRole?,
        @RequestParam(name = "inflowSource", required = false) inflowSource: UserInflowSource?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminGeneralMemberResponse>>> {
        val result = adminMemberService.getGeneralMembers(
            condition = memberSearchCondition(keyword, status, joinedFrom, joinedTo, role, inflowSource),
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.members.map(AdminGeneralMemberResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{userId}")
    override fun getGeneralMember(
        @PathVariable("userId") userId: Long,
    ): ResponseEntity<SuccessResponse<AdminGeneralMemberDetailResponse>> =
        SuccessResponse.ok(AdminGeneralMemberDetailResponse.from(adminMemberService.getGeneralMember(userId)))

    @PatchMapping("/{userId}/role")
    override fun changeGeneralMemberRole(
        @AuthenticationPrincipal adminUserId: Long,
        @PathVariable("userId") userId: Long,
        @RequestBody request: ChangeGeneralMemberRoleRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminMemberService.changeGeneralMemberRole(adminUserId, userId, request.generalMemberRole())
        return SuccessResponse.ok()
    }
}
