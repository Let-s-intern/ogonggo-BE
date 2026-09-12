package com.ogonggo.userapi.community.presentation

import com.ogonggo.userapi.community.business.RecruitmentPostCommentService
import com.ogonggo.userapi.community.presentation.request.CreateRecruitmentPostCommentRequest
import com.ogonggo.userapi.community.presentation.response.CreateRecruitmentPostCommentResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostCommentPageResponse
import com.ogonggo.userapi.community.presentation.response.RecruitmentPostCommentReplyPageResponse
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
        cursor: String?,
        size: Int,
    ): ResponseEntity<SuccessResponse<RecruitmentPostCommentPageResponse>> =
        SuccessResponse.ok(
            RecruitmentPostCommentPageResponse.from(
                result = recruitmentPostCommentService.readComments(
                    userId = userId,
                    postId = postId,
                    cursor = RecruitmentPostCommentCursorCodec.decode(cursor),
                    size = size,
                ),
                cursorEncoder = RecruitmentPostCommentCursorCodec::encode,
            ),
        )

    override fun getReplies(
        userId: Long?,
        postId: Long,
        commentId: Long,
        cursor: String?,
        size: Int,
    ): ResponseEntity<SuccessResponse<RecruitmentPostCommentReplyPageResponse>> =
        SuccessResponse.ok(
            RecruitmentPostCommentReplyPageResponse.from(
                result = recruitmentPostCommentService.readReplies(
                    userId = userId,
                    postId = postId,
                    parentId = commentId,
                    cursor = RecruitmentPostCommentCursorCodec.decode(cursor),
                    size = size,
                ),
                cursorEncoder = RecruitmentPostCommentCursorCodec::encode,
            ),
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
