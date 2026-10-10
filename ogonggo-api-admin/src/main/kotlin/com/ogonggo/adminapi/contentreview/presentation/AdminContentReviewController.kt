package com.ogonggo.adminapi.contentreview.presentation

import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.adminapi.contentreview.business.AdminContentReviewService
import com.ogonggo.adminapi.contentreview.presentation.request.DecideReviewRequest
import com.ogonggo.adminapi.contentreview.presentation.response.AdminContentReviewDecisionResponse
import com.ogonggo.adminapi.contentreview.presentation.response.AdminContentReviewItemResponse
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/content-reviews")
class AdminContentReviewController(
    private val adminReviewService: AdminContentReviewService,
) : AdminContentReviewApi {

    @GetMapping
    override fun getQueue(): ResponseEntity<SuccessResponse<List<AdminContentReviewItemResponse>>> =
        SuccessResponse.ok(adminReviewService.getQueue().map(AdminContentReviewItemResponse::from))

    @PatchMapping("/{type}/{id}")
    override fun decide(
        @PathVariable("type") type: String,
        @PathVariable("id") id: Long,
        @RequestBody request: DecideReviewRequest,
    ): ResponseEntity<SuccessResponse<AdminContentReviewDecisionResponse>> {
        val contentType = parseContentReviewTargetType(type)
        val result = when (request.decision) {
            ContentReviewStatus.APPROVED -> adminReviewService.approve(contentType, id)
            ContentReviewStatus.REJECTED -> adminReviewService.reject(contentType, id, request.requiredReason())
            ContentReviewStatus.PENDING -> request.invalidDecision()
        }
        return SuccessResponse.ok(AdminContentReviewDecisionResponse.from(result))
    }

    @PatchMapping("/{type}/{id}/undo")
    override fun undo(
        @PathVariable("type") type: String,
        @PathVariable("id") id: Long,
    ): ResponseEntity<SuccessResponse<AdminContentReviewDecisionResponse>> =
        SuccessResponse.ok(
            AdminContentReviewDecisionResponse.from(adminReviewService.undo(parseContentReviewTargetType(type), id)),
        )
}
