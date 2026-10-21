package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.job.business.AiJobService
import com.ogonggo.adminapi.job.presentation.request.AiJobRegistrationRequest
import com.ogonggo.adminapi.job.presentation.request.AiJobReplaceRequest
import com.ogonggo.adminapi.job.presentation.request.AiJobSourceHtmlRequest
import com.ogonggo.adminapi.job.presentation.response.AiJobLookupResponse
import com.ogonggo.adminapi.job.presentation.response.AiJobRegistrationResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
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
@RequestMapping("/api/v1/internal/jobs")
class AiJobController(
    private val aiJobService: AiJobService,
) : AiJobApi {

    @PostMapping
    override fun registerJob(
        @RequestBody request: AiJobRegistrationRequest,
    ): ResponseEntity<SuccessResponse<AiJobRegistrationResponse>> =
        SuccessResponse.created(
            AiJobRegistrationResponse(jobId = aiJobService.register(request.toCommand())),
        )

    @GetMapping
    override fun getJob(
        @RequestParam(name = "sourceUrl") sourceUrl: String,
    ): ResponseEntity<SuccessResponse<AiJobLookupResponse>> =
        SuccessResponse.ok(AiJobLookupResponse(jobId = aiJobService.getJobId(sourceUrl)))

    @PutMapping("/{jobId}")
    override fun replaceJob(
        @PathVariable("jobId") jobId: Long,
        @RequestBody request: AiJobReplaceRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        aiJobService.replace(jobId, request.toCommand())
        return SuccessResponse.ok()
    }

    @PutMapping("/{jobId}/source-html")
    override fun changeSourceHtml(
        @PathVariable("jobId") jobId: Long,
        @RequestBody request: AiJobSourceHtmlRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        aiJobService.changeSourceHtml(jobId, request.sourceHtml)
        return SuccessResponse.ok()
    }

    @PostMapping("/{jobId}/hide")
    override fun hideJob(
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        aiJobService.hide(jobId)
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{jobId}")
    override fun deleteJob(
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        aiJobService.delete(jobId)
        return SuccessResponse.ok()
    }
}
