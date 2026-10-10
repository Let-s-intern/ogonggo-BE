package com.ogonggo.adminapi.letscareercontent.presentation

import com.ogonggo.adminapi.letscareercontent.business.AdminLetsCareerContentRecommendService
import com.ogonggo.adminapi.letscareercontent.presentation.request.ReplaceAdminLetsCareerContentRecommendWeightsRequest
import com.ogonggo.adminapi.letscareercontent.presentation.response.AdminLetsCareerContentRecommendWeightsResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/lets-career-content-recommend-weights")
class AdminLetsCareerContentRecommendController(
    private val adminLetsCareerContentRecommendService: AdminLetsCareerContentRecommendService,
) : AdminLetsCareerContentRecommendApi {

    @GetMapping
    override fun getWeights(): ResponseEntity<SuccessResponse<AdminLetsCareerContentRecommendWeightsResponse>> =
        SuccessResponse.ok(AdminLetsCareerContentRecommendWeightsResponse.from(adminLetsCareerContentRecommendService.getWeights()))

    @PutMapping
    override fun replaceWeights(
        @RequestBody request: ReplaceAdminLetsCareerContentRecommendWeightsRequest,
    ): ResponseEntity<SuccessResponse<AdminLetsCareerContentRecommendWeightsResponse>> {
        adminLetsCareerContentRecommendService.replaceWeights(request.toWeights())
        return SuccessResponse.ok(AdminLetsCareerContentRecommendWeightsResponse.from(adminLetsCareerContentRecommendService.getWeights()))
    }
}
