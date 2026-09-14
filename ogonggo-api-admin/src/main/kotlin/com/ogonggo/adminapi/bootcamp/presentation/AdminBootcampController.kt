package com.ogonggo.adminapi.bootcamp.presentation

import com.ogonggo.adminapi.bootcamp.business.AdminBootcampService
import com.ogonggo.adminapi.bootcamp.presentation.request.UpdateAdminBootcampRequest
import com.ogonggo.adminapi.bootcamp.presentation.response.AdminBootcampDetailResponse
import com.ogonggo.adminapi.bootcamp.presentation.response.AdminBootcampSummaryResponse
import com.ogonggo.adminapi.content.business.AdminContentSortType
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestParameterException
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.bootcamp.domain.BootcampManagementSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/bootcamps")
class AdminBootcampController(
    private val adminBootcampService: AdminBootcampService,
) : AdminBootcampApi {

    @GetMapping
    override fun getBootcamps(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "sort", defaultValue = "REGISTERED_AT") sortType: AdminContentSortType,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "visibility", required = false) visibility: AdminContentVisibility?,
        @RequestParam(name = "source", required = false) source: ContentSource?,
        @RequestParam(name = "reviewStatus", required = false) reviewStatus: ReviewStatus?,
        @RequestParam(name = "status", required = false) status: BootcampStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminBootcampSummaryResponse>>> {
        // 임시저장은 콘솔에서 만들 수 있는 상태가 아니라 필터로 두면 골라도 늘 0건이다.
        if (status == BootcampStatus.DRAFT) {
            throw InvalidRequestParameterException("status", "RECRUITING 또는 CLOSED만 고를 수 있습니다.")
        }
        val result = adminBootcampService.getBootcamps(
            condition = BootcampManagementSearchCondition(
                published = visibility?.published,
                source = source,
                reviewStatus = reviewStatus,
                status = status,
                // 프런트는 빈 필터를 보내지 않지만, 빈 값이 와도 전체로 본다.
                keyword = keyword?.takeIf { it.isNotBlank() },
            ),
            sortType = sortType.bootcampSortType,
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdminBootcampSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{bootcampId}")
    override fun getBootcamp(
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<AdminBootcampDetailResponse>> =
        SuccessResponse.ok(AdminBootcampDetailResponse.from(adminBootcampService.getBootcamp(bootcampId)))

    /** 수정이 커밋된 뒤의 부트캠프를 다시 읽어 응답한다. */
    @PatchMapping("/{bootcampId}")
    override fun updateBootcamp(
        @PathVariable("bootcampId") bootcampId: Long,
        @RequestBody request: UpdateAdminBootcampRequest,
    ): ResponseEntity<SuccessResponse<AdminBootcampDetailResponse>> {
        adminBootcampService.updateBootcamp(bootcampId, request.toCommand())
        return SuccessResponse.ok(AdminBootcampDetailResponse.from(adminBootcampService.getBootcamp(bootcampId)))
    }

    @DeleteMapping("/{bootcampId}")
    override fun deleteBootcamp(
        @PathVariable("bootcampId") bootcampId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminBootcampService.deleteBootcamp(bootcampId)
        return SuccessResponse.ok()
    }
}
