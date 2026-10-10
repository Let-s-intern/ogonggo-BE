package com.ogonggo.adminapi.letscareercontent.presentation

import com.ogonggo.adminapi.letscareercontent.business.CrawlerLetsCareerContentTagService
import com.ogonggo.adminapi.letscareercontent.presentation.request.CrawlerLetsCareerContentTagRequest
import com.ogonggo.adminapi.letscareercontent.presentation.response.CrawlerLetsCareerContentTagTargetResponse
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
class CrawlerLetsCareerContentTagController(
    private val crawlerLetsCareerContentTagService: CrawlerLetsCareerContentTagService,
) : CrawlerLetsCareerContentTagApi {

    @GetMapping("/tag-targets")
    override fun getTagTargets(
        @RequestParam(name = "size", defaultValue = CrawlerLetsCareerContentTagApi.DEFAULT_TARGETS.toString()) size: Int,
    ): ResponseEntity<SuccessResponse<List<CrawlerLetsCareerContentTagTargetResponse>>> =
        SuccessResponse.ok(
            crawlerLetsCareerContentTagService.getTargets(size).map(CrawlerLetsCareerContentTagTargetResponse::from),
        )

    @PutMapping("/{contentId}/tags")
    override fun replaceTags(
        @PathVariable("contentId") contentId: Long,
        @RequestBody request: CrawlerLetsCareerContentTagRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        crawlerLetsCareerContentTagService.saveTags(contentId, request.contentHash, request.toTags())
        return SuccessResponse.ok()
    }
}
