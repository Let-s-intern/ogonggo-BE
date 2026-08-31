package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.job.business.CrawlerJobService
import com.ogonggo.adminapi.job.presentation.request.CrawlerJobRegistrationRequest
import com.ogonggo.adminapi.job.presentation.response.CrawlerJobRegistrationResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class CrawlerJobController(
    private val crawlerJobService: CrawlerJobService,
) : CrawlerJobApi {

    override fun registerJob(
        request: CrawlerJobRegistrationRequest,
    ): ResponseEntity<SuccessResponse<CrawlerJobRegistrationResponse>> =
        SuccessResponse.created(
            CrawlerJobRegistrationResponse(jobId = crawlerJobService.register(request.toCommand())),
        )
}
