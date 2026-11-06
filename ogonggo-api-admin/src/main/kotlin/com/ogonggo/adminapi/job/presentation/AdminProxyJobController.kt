package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.job.business.AdminJobService
import com.ogonggo.adminapi.job.business.AdminProxyJobService
import com.ogonggo.adminapi.job.presentation.request.CreateProxyJobRequest
import com.ogonggo.adminapi.job.presentation.response.AdminJobDetailResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/company-members/{userId}/jobs")
class AdminProxyJobController(
    private val adminProxyJobService: AdminProxyJobService,
    private val adminJobService: AdminJobService,
) : AdminProxyJobApi {

    /** 등록이 커밋된 뒤의 공고를 다시 읽어 응답한다. */
    @PostMapping
    override fun createCompanyMemberJob(
        @PathVariable("userId") userId: Long,
        @RequestBody request: CreateProxyJobRequest,
        @AuthenticationPrincipal adminUserId: Long,
    ): ResponseEntity<SuccessResponse<AdminJobDetailResponse>> {
        val jobId = adminProxyJobService.create(userId, request.toCommand(), adminUserId)
        return SuccessResponse.created(AdminJobDetailResponse.from(adminJobService.getJob(jobId)))
    }
}
