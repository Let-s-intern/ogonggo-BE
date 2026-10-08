package com.ogonggo.userapi.concern.presentation.response

import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.userapi.concern.business.ConcernAuthorResult
import com.ogonggo.userapi.concern.business.ConcernDetailResult
import com.ogonggo.userapi.concern.business.ConcernPageResult
import com.ogonggo.userapi.concern.business.ConcernSummaryResult
import com.ogonggo.userapi.response.PageResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class CreateConcernResponse(
    val id: Long,
)

data class ConcernAuthorResponse(
    @field:Schema(description = "렛츠커리어 프로필 닉네임. 프로필이 없으면 null")
    val nickname: String?,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(result: ConcernAuthorResult) = ConcernAuthorResponse(
            nickname = result.nickname,
            profileImageUrl = result.profileImageUrl,
        )
    }
}

data class ConcernSummaryResponse(
    val id: Long,
    val category: ConcernCategory,
    val title: String,
    @field:Schema(description = "본문 전체. 미리보기 줄임은 클라이언트가 합니다.")
    val content: String,
    val author: ConcernAuthorResponse,
    val createdAt: LocalDateTime,
    val viewCount: Long,
    @field:Schema(description = "남아 있는 답변(부모 댓글) 수. 답글은 세지 않습니다.")
    val commentCount: Long,
    @field:Schema(description = "삭제되지 않은 운영자 답변이 있으면 true (오공고 답변 배지)")
    val hasOfficialComment: Boolean,
) {
    companion object {
        fun from(result: ConcernSummaryResult) = ConcernSummaryResponse(
            id = result.id,
            category = result.category,
            title = result.title,
            content = result.content,
            author = ConcernAuthorResponse.from(result.author),
            createdAt = result.createdAt,
            viewCount = result.viewCount,
            commentCount = result.commentCount,
            hasOfficialComment = result.hasOfficialComment,
        )
    }
}

data class ConcernDetailResponse(
    val id: Long,
    val category: ConcernCategory,
    val title: String,
    val content: String,
    val author: ConcernAuthorResponse,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val viewCount: Long,
    @field:Schema(description = "남아 있는 답변(부모 댓글) 수. 답글은 세지 않습니다.")
    val commentCount: Long,
    val hasOfficialComment: Boolean,
    @field:Schema(description = "로그인한 사용자가 쓴 고민글이면 true")
    val mine: Boolean,
) {
    companion object {
        fun from(result: ConcernDetailResult) = ConcernDetailResponse(
            id = result.id,
            category = result.category,
            title = result.title,
            content = result.content,
            author = ConcernAuthorResponse.from(result.author),
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
            viewCount = result.viewCount,
            commentCount = result.commentCount,
            hasOfficialComment = result.hasOfficialComment,
            mine = result.mine,
        )
    }
}

internal fun ConcernPageResult.toPageResponse(): PageResponse<ConcernSummaryResponse> = PageResponse.fromZeroBased(
    items = items.map(ConcernSummaryResponse::from),
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)
