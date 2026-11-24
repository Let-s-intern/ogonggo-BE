package com.ogonggo.userapi.featureflag.presentation

import com.ogonggo.userapi.featureflag.business.UserFeatureFlagService
import com.ogonggo.userapi.featureflag.presentation.response.UserFeatureFlagListResponse
import com.ogonggo.userapi.featureflag.presentation.response.UserFeatureFlagResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/feature-flags")
class UserFeatureFlagController(
    private val userFeatureFlagService: UserFeatureFlagService,
) : UserFeatureFlagApi {

    @GetMapping
    override fun getFeatureFlags(): ResponseEntity<SuccessResponse<UserFeatureFlagListResponse>> =
        SuccessResponse.ok(
            UserFeatureFlagListResponse(
                items = userFeatureFlagService.getFeatureFlags().map(UserFeatureFlagResponse::from),
            ),
        )
}
