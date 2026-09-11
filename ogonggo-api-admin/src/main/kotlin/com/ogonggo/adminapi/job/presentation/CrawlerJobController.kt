package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.job.business.CrawlerJobService
import com.ogonggo.adminapi.job.presentation.request.CrawlerJobRegistrationRequest
import com.ogonggo.adminapi.job.presentation.response.CrawlerJobRegistrationResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
}
