package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.job.business.CrawlerJobService
import com.ogonggo.adminapi.job.presentation.request.CrawlerJobRegistrationRequest
import com.ogonggo.adminapi.job.presentation.request.CrawlerJobReplaceRequest
import com.ogonggo.adminapi.job.presentation.response.CrawlerJobLookupResponse
import com.ogonggo.adminapi.job.presentation.response.CrawlerJobRegistrationResponse
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
class CrawlerJobController(
    private val crawlerJobService: CrawlerJobService,
) : CrawlerJobApi {

    @PostMapping
    override fun registerJob(
        @RequestBody request: CrawlerJobRegistrationRequest,
    ): ResponseEntity<SuccessResponse<CrawlerJobRegistrationResponse>> =
        SuccessResponse.created(
            CrawlerJobRegistrationResponse(jobId = crawlerJobService.register(request.toCommand())),
        )

    @GetMapping
    override fun getJob(
        @RequestParam(name = "sourceUrl") sourceUrl: String,
    ): ResponseEntity<SuccessResponse<CrawlerJobLookupResponse>> =
        SuccessResponse.ok(CrawlerJobLookupResponse(jobId = crawlerJobService.getJobId(sourceUrl)))

    @PutMapping("/{jobId}")
    override fun replaceJob(
        @PathVariable("jobId") jobId: Long,
        @RequestBody request: CrawlerJobReplaceRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        crawlerJobService.replace(jobId, request.toCommand())
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{jobId}")
    override fun deleteJob(
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        crawlerJobService.delete(jobId)
        return SuccessResponse.ok()
    }
}
