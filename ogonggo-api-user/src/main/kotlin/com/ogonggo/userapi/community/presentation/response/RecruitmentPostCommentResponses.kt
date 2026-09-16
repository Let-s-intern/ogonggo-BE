package com.ogonggo.userapi.community.presentation.response

import com.ogonggo.core.community.implement.RecruitmentPostCommentCursor
import com.ogonggo.userapi.community.business.RecruitmentPostCommentAuthorResult
import com.ogonggo.userapi.community.business.RecruitmentPostCommentPageResult
import com.ogonggo.userapi.community.business.RecruitmentPostCommentReplyPageResult
import com.ogonggo.userapi.community.business.RecruitmentPostCommentResult
import com.ogonggo.userapi.community.business.RecruitmentPostCommentRootResult
import java.time.LocalDateTime

data class RecruitmentPostCommentPageResponse(
    val items: List<RecruitmentPostCommentRootResponse>,
    val nextCursor: String?,
    val hasNext: Boolean,
) {
    companion object {
        fun from(
            result: RecruitmentPostCommentPageResult,
            cursorEncoder: (RecruitmentPostCommentCursor) -> String,
        ): RecruitmentPostCommentPageResponse = RecruitmentPostCommentPageResponse(
            items = result.items.map { RecruitmentPostCommentRootResponse.from(it, cursorEncoder) },
            nextCursor = result.nextCursor?.let(cursorEncoder),
            hasNext = result.hasNext,
        )
    }
}

data class RecruitmentPostCommentRootResponse(
    val id: Long,
    val parentId: Long?,
    val author: RecruitmentPostCommentAuthorResponse,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val mine: Boolean,
    val replies: RecruitmentPostCommentReplyPageResponse,
) {
    companion object {
        fun from(
            result: RecruitmentPostCommentRootResult,
            cursorEncoder: (RecruitmentPostCommentCursor) -> String,
        ): RecruitmentPostCommentRootResponse = RecruitmentPostCommentRootResponse(
            id = result.comment.id,
            parentId = result.comment.parentId,
            author = RecruitmentPostCommentAuthorResponse.from(result.comment.author),
            content = result.comment.content,
            createdAt = result.comment.createdAt,
            updatedAt = result.comment.updatedAt,
            mine = result.comment.mine,
            replies = RecruitmentPostCommentReplyPageResponse.from(result.replies, cursorEncoder),
        )
    }
}

data class RecruitmentPostCommentReplyPageResponse(
    val items: List<RecruitmentPostCommentResponse>,
    val nextCursor: String?,
    val hasNext: Boolean,
) {
    companion object {
        fun from(
            result: RecruitmentPostCommentReplyPageResult,
            cursorEncoder: (RecruitmentPostCommentCursor) -> String,
        ): RecruitmentPostCommentReplyPageResponse = RecruitmentPostCommentReplyPageResponse(
            items = result.items.map(RecruitmentPostCommentResponse::from),
            nextCursor = result.nextCursor?.let(cursorEncoder),
            hasNext = result.hasNext,
        )
    }
}

data class RecruitmentPostCommentResponse(
    val id: Long,
    val parentId: Long?,
    val author: RecruitmentPostCommentAuthorResponse,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val mine: Boolean,
) {
    companion object {
        fun from(result: RecruitmentPostCommentResult): RecruitmentPostCommentResponse = RecruitmentPostCommentResponse(
            id = result.id,
            parentId = result.parentId,
            author = RecruitmentPostCommentAuthorResponse.from(result.author),
            content = result.content,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
            mine = result.mine,
        )
    }
}

data class RecruitmentPostCommentAuthorResponse(
    val userId: Long,
    val nickname: String?,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(result: RecruitmentPostCommentAuthorResult): RecruitmentPostCommentAuthorResponse =
            RecruitmentPostCommentAuthorResponse(
                userId = result.userId,
                nickname = result.nickname,
                profileImageUrl = result.profileImageUrl,
            )
    }
}
