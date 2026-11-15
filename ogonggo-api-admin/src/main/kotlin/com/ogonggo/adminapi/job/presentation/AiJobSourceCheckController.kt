package com.ogonggo.adminapi.job.presentation

import com.ogonggo.adminapi.job.business.AiJobSourceCheckService
import com.ogonggo.adminapi.job.presentation.response.AiJobSourceCheckTargetResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/internal/jobs")
class AiJobSourceCheckController(
    private val aiJobSourceCheckService: AiJobSourceCheckService,
) : AiJobSourceCheckApi {

    @GetMapping("/source-check-targets")
    override fun getSourceCheckTargets(
        @RequestParam(name = "afterId", defaultValue = "0") afterId: Long,
        @RequestParam(name = "size", defaultValue = AiJobSourceCheckApi.DEFAULT_TARGETS.toString()) size: Int,
    ): ResponseEntity<SuccessResponse<List<AiJobSourceCheckTargetResponse>>> =
        SuccessResponse.ok(
            aiJobSourceCheckService.getTargets(afterId, size).map(AiJobSourceCheckTargetResponse::from),
        )

    @PostMapping("/{jobId}/source-check-close")
    override fun closeBySourceCheck(
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        aiJobSourceCheckService.close(jobId)
        return SuccessResponse.ok()
    }
}
