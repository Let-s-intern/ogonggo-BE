package com.ogonggo.userapi.servicefeedback.presentation

import com.ogonggo.userapi.servicefeedback.business.UserServiceFeedbackService
import com.ogonggo.userapi.servicefeedback.presentation.request.CreateServiceFeedbackRequest
import com.ogonggo.userapi.servicefeedback.presentation.response.CreateServiceFeedbackResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/service-feedbacks")
class UserServiceFeedbackController(
    private val userServiceFeedbackService: UserServiceFeedbackService,
) : UserServiceFeedbackApi {

    @PostMapping
    override fun createServiceFeedback(
        @AuthenticationPrincipal userId: Long?,
        @RequestBody request: CreateServiceFeedbackRequest,
    ): ResponseEntity<SuccessResponse<CreateServiceFeedbackResponse>> =
        SuccessResponse.created(
            CreateServiceFeedbackResponse(userServiceFeedbackService.createServiceFeedback(userId, request.toCommand())),
        )
}
