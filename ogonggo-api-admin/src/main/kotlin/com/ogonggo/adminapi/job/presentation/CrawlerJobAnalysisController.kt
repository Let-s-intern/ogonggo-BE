package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.job.business.CrawlerJobAnalysisService
import com.ogonggo.adminapi.job.presentation.request.CrawlerJobAnalysisRequest
import com.ogonggo.adminapi.job.presentation.response.CrawlerJobAnalysisTargetResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/internal/jobs")
class CrawlerJobAnalysisController(
    private val crawlerJobAnalysisService: CrawlerJobAnalysisService,
) : CrawlerJobAnalysisApi {

    @GetMapping("/analysis-targets")
    override fun getAnalysisTargets(
        @RequestParam(name = "size", defaultValue = CrawlerJobAnalysisApi.DEFAULT_TARGETS.toString()) size: Int,
    ): ResponseEntity<SuccessResponse<List<CrawlerJobAnalysisTargetResponse>>> =
        SuccessResponse.ok(crawlerJobAnalysisService.getTargets(size).map(CrawlerJobAnalysisTargetResponse::from))

    @PutMapping("/{jobId}/analysis")
    override fun replaceAnalysis(
        @PathVariable("jobId") jobId: Long,
        @RequestBody request: CrawlerJobAnalysisRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        crawlerJobAnalysisService.saveAnalysis(jobId, request.toCommand())
        return SuccessResponse.ok()
    }
}
