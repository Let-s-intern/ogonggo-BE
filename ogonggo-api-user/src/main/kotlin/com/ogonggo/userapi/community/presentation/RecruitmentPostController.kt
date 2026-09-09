package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.business.RecruitmentPostService
import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostRequest
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class RecruitmentPostController(
    private val recruitmentPostService: RecruitmentPostService,
) : RecruitmentPostApi {

    override fun createRecruitmentPost(
        userId: Long,
        request: CreateRecruitmentPostRequest,
    ): ResponseEntity<SuccessResponse<CreateRecruitmentPostResponse>> =
        SuccessResponse.created(
            CreateRecruitmentPostResponse(recruitmentPostService.create(userId, request.toCommand(userId))),
        )
}
