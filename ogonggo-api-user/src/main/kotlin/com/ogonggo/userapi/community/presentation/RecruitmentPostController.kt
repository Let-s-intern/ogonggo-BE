package com.ogonggo.userapi.community.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.userapi.community.business.RecruitmentPostService
import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostRequest
import com.ogonggo.userapi.community.presentation.request.RecruitmentPostListRequest
import com.ogonggo.userapi.community.presentation.request.UpdateRecruitmentPostRequest
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
    private val objectMapper: ObjectMapper,
) : RecruitmentPostApi {

    override fun getRecruitmentPost(
        postId: Long,
    ): ResponseEntity<SuccessResponse<RecruitmentPostDetailResponse>> =
        SuccessResponse.ok(
            RecruitmentPostDetailResponse.from(
                recruitmentPostService.getRecruitmentPost(postId),
                objectMapper,
            ),
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

    override fun updateRecruitmentPost(
        userId: Long,
        postId: Long,
        request: UpdateRecruitmentPostRequest,
    ): ResponseEntity<SuccessResponse<Unit>> {
        recruitmentPostService.update(userId, postId, request.toCommand())
        return SuccessResponse.ok()
    }

    override fun deleteRecruitmentPost(
        userId: Long,
        postId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        recruitmentPostService.delete(userId, postId)
        return SuccessResponse.ok()
    }
}
