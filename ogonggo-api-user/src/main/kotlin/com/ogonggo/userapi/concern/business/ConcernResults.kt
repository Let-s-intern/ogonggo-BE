package com.ogonggo.userapi.concern.business

import com.ogonggo.core.concern.domain.ConcernCategory
import java.time.LocalDateTime

data class ConcernAuthorResult(
    val nickname: String?,
    val profileImageUrl: String?,
)

data class ConcernSummaryResult(
    val id: Long,
    val category: ConcernCategory,
    val title: String,
    val content: String,
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
    val author: ConcernAuthorResult,
    val official: Boolean,
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
