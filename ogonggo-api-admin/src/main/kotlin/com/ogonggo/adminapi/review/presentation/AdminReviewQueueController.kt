package com.ogonggo.adminapi.review.presentation

import com.ogonggo.adminapi.response.SuccessResponse
import com.ogonggo.adminapi.review.business.AdminReviewService
import com.ogonggo.adminapi.review.presentation.request.DecideReviewRequest
import com.ogonggo.adminapi.review.presentation.response.AdminReviewDecisionResponse
import com.ogonggo.adminapi.review.presentation.response.AdminReviewItemResponse
import com.ogonggo.core.review.domain.ReviewStatus
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
@RequestMapping("/api/v1/admin/review-queue")
class AdminReviewQueueController(
    private val adminReviewService: AdminReviewService,
) : AdminReviewQueueApi {

    @GetMapping
    override fun getQueue(): ResponseEntity<SuccessResponse<List<AdminReviewItemResponse>>> =
        SuccessResponse.ok(adminReviewService.getQueue().map(AdminReviewItemResponse::from))

    @PatchMapping("/{type}/{id}")
    override fun decide(
        @PathVariable("type") type: String,
        @PathVariable("id") id: Long,
        @RequestBody request: DecideReviewRequest,
    ): ResponseEntity<SuccessResponse<AdminReviewDecisionResponse>> {
        val contentType = parseReviewContentType(type)
        val result = when (request.decision) {
            ReviewStatus.APPROVED -> adminReviewService.approve(contentType, id)
            ReviewStatus.REJECTED -> adminReviewService.reject(contentType, id, request.requiredReason())
            ReviewStatus.PENDING -> request.invalidDecision()
        }
        return SuccessResponse.ok(AdminReviewDecisionResponse.from(result))
    }

    @PatchMapping("/{type}/{id}/undo")
    override fun undo(
        @PathVariable("type") type: String,
        @PathVariable("id") id: Long,
    ): ResponseEntity<SuccessResponse<AdminReviewDecisionResponse>> =
        SuccessResponse.ok(
            AdminReviewDecisionResponse.from(adminReviewService.undo(parseReviewContentType(type), id)),
        )
}
