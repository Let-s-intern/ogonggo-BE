package com.ogonggo.adminapi.letscareercontent.presentation

import com.ogonggo.adminapi.letscareercontent.business.AiLetsCareerContentTagService
import com.ogonggo.adminapi.letscareercontent.presentation.request.AiLetsCareerContentTagRequest
import com.ogonggo.adminapi.letscareercontent.presentation.response.AiLetsCareerContentResponse
import com.ogonggo.adminapi.letscareercontent.presentation.response.AiLetsCareerContentTagTargetResponse
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
@RequestMapping("/api/v1/internal/lets-career-contents")
class AiLetsCareerContentTagController(
    private val aiLetsCareerContentTagService: AiLetsCareerContentTagService,
) : AiLetsCareerContentTagApi {

    @GetMapping
    override fun getContents(): ResponseEntity<SuccessResponse<List<AiLetsCareerContentResponse>>> =
        SuccessResponse.ok(aiLetsCareerContentTagService.getContents().map(AiLetsCareerContentResponse::from))

    @GetMapping("/tag-targets")
    override fun getTagTargets(
        @RequestParam(name = "size", defaultValue = AiLetsCareerContentTagApi.DEFAULT_TARGETS.toString()) size: Int,
    ): ResponseEntity<SuccessResponse<List<AiLetsCareerContentTagTargetResponse>>> =
        SuccessResponse.ok(
            aiLetsCareerContentTagService.getTargets(size).map(AiLetsCareerContentTagTargetResponse::from),
        )

    @PutMapping("/{contentId}/tags")
    override fun replaceTags(
        @PathVariable("contentId") contentId: Long,
        @RequestBody request: AiLetsCareerContentTagRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        aiLetsCareerContentTagService.saveTags(contentId, request.contentHash, request.toTags())
        return SuccessResponse.ok()
    }
}
