package com.ogonggo.adminapi.servicefeedback.presentation

import com.ogonggo.adminapi.servicefeedback.business.AdminServiceFeedbackService
import com.ogonggo.adminapi.servicefeedback.presentation.response.AdminServiceFeedbackResponse
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
@RequestMapping("/api/v1/admin/service-feedbacks")
class AdminServiceFeedbackController(
    private val adminServiceFeedbackService: AdminServiceFeedbackService,
) : AdminServiceFeedbackApi {

    @GetMapping
    override fun getServiceFeedbacks(
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<AdminServiceFeedbackResponse>>> {
        val result = adminServiceFeedbackService.getServiceFeedbacks(page = page - 1, size = size)
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(AdminServiceFeedbackResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }
}
