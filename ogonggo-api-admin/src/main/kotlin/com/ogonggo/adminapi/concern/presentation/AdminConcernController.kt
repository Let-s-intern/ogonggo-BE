package com.ogonggo.adminapi.concern.presentation

import com.ogonggo.adminapi.concern.business.AdminConcernService
import com.ogonggo.adminapi.concern.presentation.request.ChangeAdminConcernVisibilityRequest
import com.ogonggo.adminapi.concern.presentation.response.AdminConcernDetailResponse
import com.ogonggo.adminapi.concern.presentation.response.AdminConcernSummaryResponse
import com.ogonggo.adminapi.content.business.AdminContentSortType
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernConsoleSearchCondition
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/concerns")
class AdminConcernController(
    private val adminConcernService: AdminConcernService,
) : AdminConcernApi {

    @GetMapping
    override fun getConcerns(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "sort", defaultValue = "REGISTERED_AT") sortType: AdminContentSortType,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "visibility", required = false) visibility: AdminContentVisibility?,
        @RequestParam(name = "category", required = false) category: ConcernCategory?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminConcernSummaryResponse>>> {
        val result = adminConcernService.getConcerns(
            condition = ConcernConsoleSearchCondition(
                visible = visibility?.published,
                category = category,
                // 프런트는 빈 필터를 보내지 않지만, 빈 값이 와도 전체로 본다.
                keyword = keyword?.takeIf { it.isNotBlank() },
            ),
            sortType = sortType.concernSortType,
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdminConcernSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{concernId}")
    override fun getConcern(
        @PathVariable("concernId") concernId: Long,
    ): ResponseEntity<SuccessResponse<AdminConcernDetailResponse>> =
        SuccessResponse.ok(AdminConcernDetailResponse.from(adminConcernService.getConcern(concernId)))

    @PatchMapping("/visibility")
    override fun changeConcernVisibilities(
        @RequestBody request: ChangeAdminConcernVisibilityRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminConcernService.changeVisibilities(request.toCommand())
        return SuccessResponse.ok()
    }
}
