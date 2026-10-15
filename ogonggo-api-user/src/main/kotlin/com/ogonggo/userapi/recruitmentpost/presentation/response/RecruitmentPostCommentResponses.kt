package com.ogonggo.userapi.recruitmentpost.presentation.response

import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostCommentAuthorResult
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostCommentPageResult
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostCommentReplyPageResult
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostCommentResult
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostCommentRootResult
import com.ogonggo.userapi.response.PageResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

internal fun RecruitmentPostCommentPageResult.toPageResponse(): PageResponse<RecruitmentPostCommentRootResponse> =
    PageResponse.fromZeroBased(
        items = items.map(RecruitmentPostCommentRootResponse::from),
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

data class RecruitmentPostCommentRootResponse(
    val id: Long,
    val parentId: Long?,
    val author: RecruitmentPostCommentAuthorResponse,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val mine: Boolean,
    val replies: PageResponse<RecruitmentPostCommentResponse>,
) {
    companion object {
        fun from(result: RecruitmentPostCommentRootResult): RecruitmentPostCommentRootResponse = RecruitmentPostCommentRootResponse(
            id = result.comment.id,
            parentId = result.comment.parentId,
            author = RecruitmentPostCommentAuthorResponse.from(result.comment.author),
            content = result.comment.content,
            createdAt = result.comment.createdAt,
            updatedAt = result.comment.updatedAt,
            mine = result.comment.mine,
            replies = result.replies.toPageResponse(),
        )
    }
}

internal fun RecruitmentPostCommentReplyPageResult.toPageResponse(): PageResponse<RecruitmentPostCommentResponse> =
    PageResponse.fromZeroBased(
        items = items.map(RecruitmentPostCommentResponse::from),
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

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
    @field:Schema(description = "작성자의 현재 역할. MENTOR는 멘토, ADMIN은 멘토보다 위 등급(관리자)으로 표시합니다.")
    val role: UserRole,
) {
    companion object {
        fun from(result: RecruitmentPostCommentAuthorResult): RecruitmentPostCommentAuthorResponse =
            RecruitmentPostCommentAuthorResponse(
                userId = result.userId,
                nickname = result.nickname,
                profileImageUrl = result.profileImageUrl,
                role = result.role,
            )
    }
}
