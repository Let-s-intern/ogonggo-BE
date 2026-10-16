package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.job.business.AiJobAnalysisService
import com.ogonggo.adminapi.job.presentation.request.AiJobAnalysisRequest
import com.ogonggo.adminapi.job.presentation.response.AiJobAnalysisTargetResponse
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
class AiJobAnalysisController(
    private val aiJobAnalysisService: AiJobAnalysisService,
) : AiJobAnalysisApi {

    @GetMapping("/analysis-targets")
    override fun getAnalysisTargets(
        @RequestParam(name = "size", defaultValue = AiJobAnalysisApi.DEFAULT_TARGETS.toString()) size: Int,
    ): ResponseEntity<SuccessResponse<List<AiJobAnalysisTargetResponse>>> =
        SuccessResponse.ok(aiJobAnalysisService.getTargets(size).map(AiJobAnalysisTargetResponse::from))

    @PutMapping("/{jobId}/analysis")
    override fun replaceAnalysis(
        @PathVariable("jobId") jobId: Long,
        @RequestBody request: AiJobAnalysisRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        aiJobAnalysisService.saveAnalysis(jobId, request.toCommand())
        return SuccessResponse.ok()
    }
}
