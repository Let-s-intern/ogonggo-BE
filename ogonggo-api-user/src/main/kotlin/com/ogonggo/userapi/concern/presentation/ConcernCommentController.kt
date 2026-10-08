package com.ogonggo.userapi.concern.presentation

import com.ogonggo.userapi.concern.business.ConcernCommentService
import com.ogonggo.userapi.concern.presentation.request.CreateConcernCommentRequest
import com.ogonggo.userapi.concern.presentation.response.ConcernCommentResponse
import com.ogonggo.userapi.concern.presentation.response.ConcernCommentRootResponse
import com.ogonggo.userapi.concern.presentation.response.CreateConcernCommentResponse
import com.ogonggo.userapi.concern.presentation.response.toPageResponse
import com.ogonggo.userapi.response.PageResponse
import com.ogonggo.userapi.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/concerns/{concernId}/comments")
class ConcernCommentController(
    private val concernCommentService: ConcernCommentService,
) : ConcernCommentApi {

    @GetMapping
    override fun listComments(
        @AuthenticationPrincipal userId: Long?,
        @PathVariable("concernId") concernId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<ConcernCommentRootResponse>>> =
        SuccessResponse.ok(
            concernCommentService.readComments(userId, concernId, page = page - 1, size = size).toPageResponse(),
        )

    @GetMapping("/{commentId}/replies")
    override fun listReplies(
        @AuthenticationPrincipal userId: Long?,
        @PathVariable("concernId") concernId: Long,
        @PathVariable("commentId") commentId: Long,
        @RequestParam(name = "page", defaultValue = "1") page: Int,
        @RequestParam(name = "size", defaultValue = "5") size: Int,
    ): ResponseEntity<SuccessResponse<PageResponse<ConcernCommentResponse>>> =
        SuccessResponse.ok(
            concernCommentService.readReplies(userId, concernId, commentId, page = page - 1, size = size)
                .toPageResponse(),
        )

    @PostMapping
    override fun createComment(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("concernId") concernId: Long,
        @RequestBody request: CreateConcernCommentRequest,
    ): ResponseEntity<SuccessResponse<CreateConcernCommentResponse>> =
        SuccessResponse.created(
            CreateConcernCommentResponse(concernCommentService.create(userId, concernId, request.toCommand())),
        )

    @DeleteMapping("/{commentId}")
    override fun deleteComment(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("concernId") concernId: Long,
        @PathVariable("commentId") commentId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        concernCommentService.delete(userId, concernId, commentId)
        return SuccessResponse.ok()
    }

    @PutMapping("/{commentId}/helpful-votes/me")
    override fun voteHelpful(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("concernId") concernId: Long,
        @PathVariable("commentId") commentId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        concernCommentService.voteHelpful(userId, concernId, commentId)
        return SuccessResponse.ok()
    }

    @DeleteMapping("/{commentId}/helpful-votes/me")
    override fun cancelHelpful(
        @AuthenticationPrincipal userId: Long,
        @PathVariable("concernId") concernId: Long,
        @PathVariable("commentId") commentId: Long,
    ): ResponseEntity<SuccessResponse<Unit>> {
        concernCommentService.cancelHelpful(userId, concernId, commentId)
        return SuccessResponse.ok()
    }
}
