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
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class CompanyJobController(
    private val companyJobService: CompanyJobService,
) : CompanyJobApi {

    override fun createJob(
        userId: Long,
        request: CreateCompanyJobRequest,
    ): ResponseEntity<SuccessResponse<CreateCompanyJobResponse>> =
        SuccessResponse.created(CreateCompanyJobResponse(companyJobService.create(userId, request.toCommand())))

    override fun getJobs(
        userId: Long,
        page: Int,
        size: Int,
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

    override fun getJob(
        userId: Long,
        jobId: Long,
    ): ResponseEntity<SuccessResponse<CompanyJobDetailResponse>> =
        SuccessResponse.ok(CompanyJobDetailResponse.from(companyJobService.getJob(userId, jobId)))

    override fun updateJob(
        userId: Long,
        jobId: Long,
        request: UpdateCompanyJobRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        companyJobService.update(userId, jobId, request.toCommand())
        return SuccessResponse.ok()
    }

    override fun publishJob(userId: Long, jobId: Long): ResponseEntity<SuccessResponse<Unit>> {
        companyJobService.publish(userId, jobId)
        return SuccessResponse.ok()
    }

    override fun closeJob(userId: Long, jobId: Long): ResponseEntity<SuccessResponse<Unit>> {
        companyJobService.close(userId, jobId)
        return SuccessResponse.ok()
    }

    override fun deleteJob(userId: Long, jobId: Long): ResponseEntity<SuccessResponse<Unit>> {
        companyJobService.delete(userId, jobId)
        return SuccessResponse.ok()
    }
}
