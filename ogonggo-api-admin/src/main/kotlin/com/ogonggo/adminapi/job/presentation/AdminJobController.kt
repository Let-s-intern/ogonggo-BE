package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.content.business.AdminContentSortType
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.job.business.AdminJobService
import com.ogonggo.adminapi.job.presentation.request.UpdateAdminJobRequest
import com.ogonggo.adminapi.job.presentation.response.AdminJobDetailResponse
import com.ogonggo.adminapi.job.presentation.response.AdminJobSummaryResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobRecruitmentStatus
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
@RequestMapping("/api/v1/admin/jobs")
class AdminJobController(
    private val adminJobService: AdminJobService,
) : AdminJobApi {

    @GetMapping
    override fun getJobs(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "sort", defaultValue = "REGISTERED_AT") sortType: AdminContentSortType,
        @RequestParam(name = "keyword", required = false) keyword: String?,
        @RequestParam(name = "visibility", required = false) visibility: AdminContentVisibility?,
        @RequestParam(name = "source", required = false) source: ContentSource?,
        @RequestParam(name = "reviewStatus", required = false) reviewStatus: ReviewStatus?,
        @RequestParam(name = "recruitmentStatus", required = false) recruitmentStatus: JobRecruitmentStatus?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminJobSummaryResponse>>> {
        val result = adminJobService.getJobs(
            condition = JobManagementSearchCondition(
                published = visibility?.published,
                source = source,
                reviewStatus = reviewStatus,
                recruitmentStatus = recruitmentStatus,
                // 프런트는 빈 필터를 보내지 않지만, 빈 값이 와도 전체로 본다.
                keyword = keyword?.takeIf { it.isNotBlank() },
            ),
            sortType = sortType.jobSortType,
            page = page - 1,
            size = size,
        )
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdminJobSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{jobId}")
    override fun getJob(
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<AdminJobDetailResponse>> =
        SuccessResponse.ok(AdminJobDetailResponse.from(adminJobService.getJob(jobId)))

    /** 수정이 커밋된 뒤의 공고를 다시 읽어 응답한다. */
    @PatchMapping("/{jobId}")
    override fun updateJob(
        @PathVariable("jobId") jobId: Long,
        @RequestBody request: UpdateAdminJobRequest,
    ): ResponseEntity<SuccessResponse<AdminJobDetailResponse>> {
        adminJobService.updateJob(jobId, request.toCommand())
        return SuccessResponse.ok(AdminJobDetailResponse.from(adminJobService.getJob(jobId)))
    }

    @DeleteMapping("/{jobId}")
    override fun deleteJob(
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminJobService.deleteJob(jobId)
        return SuccessResponse.ok()
    }
}
