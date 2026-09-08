package com.ogonggo.userapi.job.presentation

import com.ogonggo.userapi.job.business.CompanyJobService
import com.ogonggo.userapi.job.presentation.request.CreateCompanyJobRequest
import com.ogonggo.userapi.job.presentation.request.UpdateCompanyJobRequest
import com.ogonggo.userapi.job.presentation.response.CompanyJobDetailResponse
import com.ogonggo.userapi.job.presentation.response.CompanyJobSummaryResponse
import com.ogonggo.userapi.job.presentation.response.CreateCompanyJobResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/users/me/jobs")
class CompanyJobController(
    private val companyJobService: CompanyJobService,
) : CompanyJobApi {

    @PostMapping
    override fun createJob(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: CreateCompanyJobRequest,
    ): ResponseEntity<SuccessResponse<CreateCompanyJobResponse>> =
        SuccessResponse.created(CreateCompanyJobResponse(companyJobService.create(userId, request.toCommand())))

    @GetMapping
    override fun getJobs(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<CompanyJobSummaryResponse>>> {
        val result = companyJobService.getJobs(userId, page - 1, size)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(CompanyJobSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/{jobId}")
    override fun getJob(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<CompanyJobDetailResponse>> =
        SuccessResponse.ok(CompanyJobDetailResponse.from(companyJobService.getJob(userId, jobId)))

    @PutMapping("/{jobId}")
    override fun updateJob(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
        @RequestBody request: UpdateCompanyJobRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        companyJobService.update(userId, jobId, request.toCommand())
        return SuccessResponse.ok()
    }

    @PostMapping("/{jobId}/publish")
    override fun publishJob(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        companyJobService.publish(userId, jobId)
        return SuccessResponse.ok()
    }

    @PostMapping("/{jobId}/close")
    override fun closeJob(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        companyJobService.close(userId, jobId)
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{jobId}")
    override fun deleteJob(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        companyJobService.delete(userId, jobId)
        return SuccessResponse.ok()
    }
}
