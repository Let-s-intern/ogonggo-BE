package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.content.business.AdminContentSortType
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.error.InvalidRequestParameterException
import com.ogonggo.adminapi.job.business.AdminJobService
import com.ogonggo.adminapi.job.presentation.request.AiJobAnalysisRequest
import com.ogonggo.adminapi.job.presentation.request.ChangeAdminJobVisibilityRequest
import com.ogonggo.adminapi.job.presentation.request.ReplaceAdminTodayJobsRequest
import com.ogonggo.adminapi.job.presentation.request.UpdateAdminJobRequest
import com.ogonggo.adminapi.job.presentation.response.AdminJobDetailResponse
import com.ogonggo.adminapi.job.presentation.response.AdminJobSummaryResponse
import com.ogonggo.adminapi.job.presentation.response.AdminTodayJobSummaryResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.core.contentreview.domain.ContentSource
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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
        @RequestParam(name = "reviewStatus", required = false) reviewStatus: ContentReviewStatus?,
        @RequestParam(name = "recruitmentStatus", required = false) recruitmentStatus: JobRecruitmentStatus?,
        @RequestParam(name = "jobField", required = false) jobField: JobField?,
        @RequestParam(name = "jobRole", required = false) jobRoles: List<JobRole>?,
        @RequestParam(name = "registeredFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        registeredFrom: LocalDate?,
        @RequestParam(name = "registeredTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        registeredTo: LocalDate?,
        @RequestParam(name = "inspected", required = false) inspected: Boolean?,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminJobSummaryResponse>>> {
        if (registeredFrom != null && registeredTo != null && registeredFrom.isAfter(registeredTo)) {
            throw InvalidRequestParameterException("registeredFrom", "등록 기간 시작일은 종료일보다 늦을 수 없습니다.")
        }
        val result = adminJobService.getJobs(
            condition = JobManagementSearchCondition(
                published = visibility?.published,
                source = source,
                reviewStatus = reviewStatus,
                recruitmentStatus = recruitmentStatus,
                jobField = jobField,
                jobRoles = jobRoles.orEmpty().toSet(),
                // 프런트는 빈 필터를 보내지 않지만, 빈 값이 와도 전체로 본다.
                keyword = keyword?.takeIf { it.isNotBlank() },
                registeredFrom = registeredFrom,
                registeredTo = registeredTo,
                inspected = inspected,
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

    @GetMapping("/today")
    override fun getTodayJobs(): ResponseEntity<SuccessResponse<List<AdminTodayJobSummaryResponse>>> =
        SuccessResponse.ok(adminJobService.getTodayJobs().map(AdminTodayJobSummaryResponse::from))

    /** 교체가 커밋된 뒤의 목록을 다시 읽어 응답한다. */
    @PutMapping("/today")
    override fun replaceTodayJobs(
        @RequestBody request: ReplaceAdminTodayJobsRequest,
    ): ResponseEntity<SuccessResponse<List<AdminTodayJobSummaryResponse>>> {
        adminJobService.replaceTodayJobs(request.toDtos())
        return SuccessResponse.ok(adminJobService.getTodayJobs().map(AdminTodayJobSummaryResponse::from))
    }

    @PatchMapping("/visibility")
    override fun changeJobVisibilities(
        @RequestBody request: ChangeAdminJobVisibilityRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminJobService.changeVisibilities(request.toCommand())
        return SuccessResponse.ok()
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

    /** 저장이 커밋된 뒤의 공고를 다시 읽어 응답한다. */
    @PutMapping("/{jobId}/analysis")
    override fun replaceJobAnalysis(
        @PathVariable("jobId") jobId: Long,
        @RequestBody request: AiJobAnalysisRequest.Analysis,
        @AuthenticationPrincipal adminUserId: Long,
    ): ResponseEntity<SuccessResponse<AdminJobDetailResponse>> {
        adminJobService.replaceAnalysis(jobId, request.toContent(), adminUserId)
        return SuccessResponse.ok(AdminJobDetailResponse.from(adminJobService.getJob(jobId)))
    }

    @PutMapping("/{jobId}/inspection")
    override fun markJobInspected(
        @PathVariable("jobId") jobId: Long,
        @AuthenticationPrincipal adminUserId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminJobService.markInspected(jobId, adminUserId)
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{jobId}/inspection")
    override fun clearJobInspection(
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminJobService.clearInspection(jobId)
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{jobId}")
    override fun deleteJob(
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminJobService.deleteJob(jobId)
        return SuccessResponse.ok()
    }
}
