package com.ogonggo.adminapi.member.presentation

import com.ogonggo.adminapi.member.business.AdminMemberService
import com.ogonggo.adminapi.member.presentation.response.AdminCompanyMemberResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.user.domain.UserStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/api/v1/admin/company-members")
class AdminCompanyMemberController(
    private val adminMemberService: AdminMemberService,
) : AdminCompanyMemberApi {

    @GetMapping
    override fun getCompanyMembers(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "status", required = false) status: UserStatus?,
        @RequestParam(name = "joinedFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        joinedFrom: LocalDate?,
        @RequestParam(name = "joinedTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        joinedTo: LocalDate?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminCompanyMemberResponse>>> {
        val result = adminMemberService.getCompanyMembers(
            condition = memberSearchCondition(keyword, status, joinedFrom, joinedTo),
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.members.map(AdminCompanyMemberResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }
}
