package com.ogonggo.userapi.letscareercontent.presentation

import com.ogonggo.userapi.letscareercontent.business.UserLetsCareerContentService
import com.ogonggo.userapi.letscareercontent.presentation.response.UserRecommendedLetsCareerContentResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class UserLetsCareerContentController(
    private val userLetsCareerContentService: UserLetsCareerContentService,
) : UserLetsCareerContentApi {

    @GetMapping("/api/v1/jobs/{jobId}/recommended-lets-career-contents")
    override fun getRecommendedContents(
        @PathVariable("jobId") jobId: Long,
    ): ResponseEntity<SuccessResponse<List<UserRecommendedLetsCareerContentResponse>>> =
        SuccessResponse.ok(
            userLetsCareerContentService.getRecommended(jobId).map(UserRecommendedLetsCareerContentResponse::from),
        )
}
