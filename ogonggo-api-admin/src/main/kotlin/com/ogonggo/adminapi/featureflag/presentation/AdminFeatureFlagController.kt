package com.ogonggo.adminapi.featureflag.presentation

import com.ogonggo.adminapi.featureflag.business.AdminFeatureFlagService
import com.ogonggo.adminapi.featureflag.presentation.request.UpdateFeatureFlagRequest
import com.ogonggo.adminapi.featureflag.presentation.response.AdminFeatureFlagListResponse
import com.ogonggo.adminapi.featureflag.presentation.response.AdminFeatureFlagResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/feature-flags")
class AdminFeatureFlagController(
    private val adminFeatureFlagService: AdminFeatureFlagService,
) : AdminFeatureFlagApi {

    @GetMapping
    override fun getFeatureFlags(): ResponseEntity<SuccessResponse<AdminFeatureFlagListResponse>> =
        SuccessResponse.ok(
            AdminFeatureFlagListResponse(
                items = adminFeatureFlagService.getFeatureFlags().map(AdminFeatureFlagResponse::from),
            ),
        )

    @PatchMapping("/{key}")
    override fun updateFeatureFlag(
        @AuthenticationPrincipal adminUserId: Long,
        @PathVariable("key") key: String,
        @RequestBody request: UpdateFeatureFlagRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        adminFeatureFlagService.changeFeatureFlagEnabled(adminUserId, key, checkNotNull(request.enabled))
        return SuccessResponse.ok()
    }
}
