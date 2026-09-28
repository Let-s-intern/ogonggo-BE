package com.ogonggo.userapi.feedback.presentation

import com.ogonggo.userapi.feedback.business.UserFeedbackService
import com.ogonggo.userapi.feedback.presentation.request.CreateFeedbackRequest
import com.ogonggo.userapi.feedback.presentation.response.CreateFeedbackResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/feedbacks")
class UserFeedbackController(
    private val userFeedbackService: UserFeedbackService,
) : UserFeedbackApi {

    @PostMapping
    override fun createFeedback(
        @AuthenticationPrincipal userId: Long?,
        @RequestBody request: CreateFeedbackRequest,
    ): ResponseEntity<SuccessResponse<CreateFeedbackResponse>> =
        SuccessResponse.created(
            CreateFeedbackResponse(userFeedbackService.createFeedback(userId, request.toCommand())),
        )
}
