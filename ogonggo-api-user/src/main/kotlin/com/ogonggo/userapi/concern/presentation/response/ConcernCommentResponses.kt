package com.ogonggo.userapi.concern.presentation.response

import com.ogonggo.userapi.concern.business.ConcernCommentPageResult
import com.ogonggo.userapi.concern.business.ConcernCommentReplyPageResult
import com.ogonggo.userapi.concern.business.ConcernCommentResult
import com.ogonggo.userapi.concern.business.ConcernCommentRootResult
import com.ogonggo.userapi.response.PageResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class CreateConcernCommentResponse(
    val id: Long,
)

data class ConcernCommentResponse(
    val id: Long,
    val parentId: Long?,
    val author: ConcernAuthorResponse,
    @field:Schema(description = "관리자가 쓴 운영자 답변이면 true (렛츠커리어 매니저 배지)")
    val official: Boolean,
    @field:Schema(description = "삭제된 댓글은 \"삭제된 댓글입니다\"")
    val content: String,
    val deleted: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val mine: Boolean,
    @field:Schema(description = "도움돼요 수")
    val helpfulCount: Long,
    @field:Schema(description = "로그인한 사용자가 도움돼요를 눌렀으면 true")
    val helpfulVoted: Boolean,
) {
    companion object {
        fun from(result: ConcernCommentResult) = ConcernCommentResponse(
            id = result.id,
            parentId = result.parentId,
            author = ConcernAuthorResponse.from(result.author),
            official = result.official,
            content = result.content,
            deleted = result.deleted,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
            mine = result.mine,
            helpfulCount = result.helpfulCount,
            helpfulVoted = result.helpfulVoted,
        )
    }
}

data class ConcernCommentRootResponse(
    val id: Long,
    val author: ConcernAuthorResponse,
    @field:Schema(description = "관리자가 쓴 운영자 답변이면 true (렛츠커리어 매니저 배지)")
    val official: Boolean,
    @field:Schema(description = "삭제된 답변은 \"삭제된 댓글입니다\". 남은 답글이 있을 때만 목록에 남습니다.")
    val content: String,
    val deleted: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val mine: Boolean,
    val helpfulCount: Long,
    val helpfulVoted: Boolean,
    @field:Schema(description = "앞쪽 답글 5개. 답글 수는 `replies.pageInfo.totalElements`입니다.")
    val replies: PageResponse<ConcernCommentResponse>,
) {
    companion object {
        fun from(result: ConcernCommentRootResult): ConcernCommentRootResponse {
            val comment = result.comment
            return ConcernCommentRootResponse(
                id = comment.id,
                author = ConcernAuthorResponse.from(comment.author),
                official = comment.official,
                content = comment.content,
                deleted = comment.deleted,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
                mine = comment.mine,
                helpfulCount = comment.helpfulCount,
                helpfulVoted = comment.helpfulVoted,
                replies = result.replies.toPageResponse(),
            )
        }
    }
}

internal fun ConcernCommentPageResult.toPageResponse(): PageResponse<ConcernCommentRootResponse> =
    PageResponse.fromZeroBased(
        items = items.map(ConcernCommentRootResponse::from),
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

internal fun ConcernCommentReplyPageResult.toPageResponse(): PageResponse<ConcernCommentResponse> =
    PageResponse.fromZeroBased(
        items = items.map(ConcernCommentResponse::from),
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )
