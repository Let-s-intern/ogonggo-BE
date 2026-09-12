package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.business.RecruitmentPostCommentService
import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostCommentRequest
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostCommentResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class RecruitmentPostCommentController(
    private val recruitmentPostCommentService: RecruitmentPostCommentService,
) : RecruitmentPostCommentApi {

    override fun createComment(
        @AuthenticationPrincipal userId: Long,
        postId: Long,
        request: CreateRecruitmentPostCommentRequest,
    ): ResponseEntity<SuccessResponse<CreateRecruitmentPostCommentResponse>> =
        SuccessResponse.created(
            CreateRecruitmentPostCommentResponse(
                recruitmentPostCommentService.create(
                    userId = userId,
                    postId = postId,
                    command = request.toCommand(),
                ),
            ),
        )
}
