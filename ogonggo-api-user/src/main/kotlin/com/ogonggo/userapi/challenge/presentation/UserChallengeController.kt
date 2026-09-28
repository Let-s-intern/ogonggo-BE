package com.ogonggo.userapi.challenge.presentation

import com.ogonggo.userapi.challenge.business.UserChallengeService
import com.ogonggo.userapi.challenge.presentation.response.UserRecommendedChallengeResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/recommended-challenges")
class UserChallengeController(
    private val userChallengeService: UserChallengeService,
) : UserChallengeApi {

    @GetMapping
    override fun getRecommendedChallenges(
        @AuthenticationPrincipal userId: Long?,
    ): ResponseEntity<SuccessResponse<List<UserRecommendedChallengeResponse>>> =
        SuccessResponse.ok(
            userChallengeService.getRecommendedChallenges(userId).map(UserRecommendedChallengeResponse::from),
        )
}
