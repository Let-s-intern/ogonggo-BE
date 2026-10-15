package com.ogonggo.userapi.concern.business

import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.user.domain.UserRole
import java.time.LocalDateTime

data class ConcernAuthorResult(
    val nickname: String?,
    val profileImageUrl: String?,
)

/** 답변·답글 작성자. 고민글 작성자와 달리 멘토·관리자를 표시하도록 현재 역할을 싣는다. */
data class ConcernCommentAuthorResult(
    val nickname: String?,
    val profileImageUrl: String?,
    val role: UserRole,
)

data class ConcernImageResult(
    val id: String,
    val url: String,
)

data class ConcernSummaryResult(
    val id: Long,
    val category: ConcernCategory,
    val title: String,
    val content: String,
    val images: List<ConcernImageResult>,
    val author: ConcernAuthorResult,
    val createdAt: LocalDateTime,
    val viewCount: Long,
    val commentCount: Long,
    val hasOfficialComment: Boolean,
)

data class ConcernPageResult(
    val items: List<ConcernSummaryResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class ConcernDetailResult(
    val id: Long,
    val category: ConcernCategory,
    val title: String,
    val content: String,
    val images: List<ConcernImageResult>,
    val author: ConcernAuthorResult,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val viewCount: Long,
    val commentCount: Long,
    val hasOfficialComment: Boolean,
    val mine: Boolean,
)

data class ConcernCommentResult(
    val id: Long,
    val parentId: Long?,
    val author: ConcernCommentAuthorResult,
    val official: Boolean,
    val concernAuthor: Boolean,
    val content: String,
    val deleted: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val mine: Boolean,
    val likeCount: Long,
    val liked: Boolean,
)

data class ConcernCommentReplyPageResult(
    val items: List<ConcernCommentResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class ConcernCommentRootResult(
    val comment: ConcernCommentResult,
    val replies: ConcernCommentReplyPageResult,
)

data class ConcernCommentPageResult(
    val items: List<ConcernCommentRootResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
