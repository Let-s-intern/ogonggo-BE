package com.ogonggo.adminapi.feedback.presentation

import com.ogonggo.adminapi.feedback.business.AdminFeedbackService
import com.ogonggo.adminapi.feedback.presentation.response.AdminFeedbackResponse
import com.ogonggo.adminapi.response.PageResponse
import com.ogonggo.adminapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/admin/feedbacks")
class AdminFeedbackController(
    private val adminFeedbackService: AdminFeedbackService,
) : AdminFeedbackApi {

    @GetMapping
    override fun getFeedbacks(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminFeedbackResponse>>> {
        val result = adminFeedbackService.getFeedbacks(page = page - 1, size = size)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdminFeedbackResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }
}
