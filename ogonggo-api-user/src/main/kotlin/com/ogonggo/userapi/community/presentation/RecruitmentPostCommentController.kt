package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.business.RecruitmentPostCommentService
import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostCommentRequest
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostCommentResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostCommentResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostCommentRootResponse
import com.ogonggo.userapi.community.presentation.response.toPageResponse
import com.ogonggo.userapi.response.PageResponse
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

    override fun getComments(
        userId: Long?,
        postId: Long,
        page: Int,
        size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostCommentRootResponse>>> =
        SuccessResponse.ok(
            recruitmentPostCommentService.readComments(
                userId = userId,
                postId = postId,
                page = page - 1,
                size = size,
            ).toPageResponse(),
        )

    override fun getReplies(
        userId: Long?,
        postId: Long,
        commentId: Long,
        page: Int,
        size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<RecruitmentPostCommentResponse>>> =
        SuccessResponse.ok(
            recruitmentPostCommentService.readReplies(
                userId = userId,
                postId = postId,
                parentId = commentId,
                page = page - 1,
                size = size,
            ).toPageResponse(),
        )

    override fun deleteComment(
        userId: Long,
        postId: Long,
        commentId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        recruitmentPostCommentService.delete(userId, postId, commentId)
        return SuccessResponse.ok()
    }

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
