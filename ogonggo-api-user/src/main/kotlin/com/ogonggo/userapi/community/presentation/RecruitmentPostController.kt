package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.business.RecruitmentPostService
import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostRequest
import com.ogonggo.userapi.community.presentation.request.RecruitmentPostListRequest
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostDetailResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostSummaryResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class RecruitmentPostController(
    private val recruitmentPostService: RecruitmentPostService,
) : RecruitmentPostApi {

    override fun getRecruitmentPost(
        postId: Long,
    ): ResponseEntity<SuccessResponse<RecruitmentPostDetailResponse>> =
        SuccessResponse.ok(
            RecruitmentPostDetailResponse.from(recruitmentPostService.getRecruitmentPost(postId)),
        )

    override fun getRecruitmentPosts(
        request: RecruitmentPostListRequest,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostSummaryResponse>>> {
        val result = recruitmentPostService.getRecruitmentPosts(request.toQuery())
        return SuccessResponse.ok(
            PageResponse.fromZeroBased(
                items = result.items.map(RecruitmentPostSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    override fun createRecruitmentPost(
        userId: Long,
        request: CreateRecruitmentPostRequest,
    ): ResponseEntity<SuccessResponse<CreateRecruitmentPostResponse>> =
        SuccessResponse.created(
            CreateRecruitmentPostResponse(recruitmentPostService.create(userId, request.toCommand(userId))),
        )
}
