package com.ogonggo.adminapi.community.presentation

import com.ogonggo.adminapi.community.business.AdminRecruitmentPostService
import com.ogonggo.adminapi.community.presentation.request.ChangeAdminRecruitmentPostVisibilityRequest
import com.ogonggo.adminapi.community.presentation.response.AdminRecruitmentPostSummaryResponse
import com.ogonggo.adminapi.content.business.AdminContentSortType
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.community.domain.RecruitmentPostConsoleSearchCondition
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/recruitment-posts")
class AdminRecruitmentPostController(
    private val adminRecruitmentPostService: AdminRecruitmentPostService,
) : AdminRecruitmentPostApi {

    @GetMapping
    override fun getRecruitmentPosts(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "sort", defaultValue = "REGISTERED_AT") sortType: AdminContentSortType,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "visibility", required = false) visibility: AdminContentVisibility?,
        @RequestParam(name = "recruitmentType", required = false) recruitmentType: RecruitmentType?,
        @RequestParam(name = "recruitmentStatus", required = false) recruitmentStatus: RecruitmentStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminRecruitmentPostSummaryResponse>>> {
        val result = adminRecruitmentPostService.getRecruitmentPosts(
            condition = RecruitmentPostConsoleSearchCondition(
                published = visibility?.published,
                recruitmentType = recruitmentType,
                recruitmentStatus = recruitmentStatus,
                // 프런트는 빈 필터를 보내지 않지만, 빈 값이 와도 전체로 본다.
                keyword = keyword?.takeIf { it.isNotBlank() },
            ),
            sortType = sortType.recruitmentPostSortType,
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdminRecruitmentPostSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @PatchMapping("/visibility")
    override fun changeRecruitmentPostVisibilities(
        @RequestBody request: ChangeAdminRecruitmentPostVisibilityRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminRecruitmentPostService.changeVisibilities(request.toCommand())
        return SuccessResponse.ok()
    }
}
