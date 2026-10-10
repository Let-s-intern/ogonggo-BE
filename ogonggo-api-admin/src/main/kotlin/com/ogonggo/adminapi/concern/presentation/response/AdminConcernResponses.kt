package com.ogonggo.adminapi.concern.presentation.response

import com.ogonggo.adminapi.concern.business.AdminConcernDetail
import com.ogonggo.adminapi.concern.business.AdminConcernSummary
import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.concern.domain.ConcernCategory
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminConcernSummaryResponse(
    val id: Long,
    val category: ConcernCategory,
    val title: String,
    val viewCount: Long,
    @Schema(description = "남아 있는 답변 수입니다. 답글은 세지 않습니다.")
    val commentCount: Long,
    @Schema(description = "삭제되지 않은 운영자 답변이 있는지입니다.")
    val hasOfficialComment: Boolean,
    val visibility: AdminContentVisibility,
    val authorUserId: Long,
    @Schema(description = "작성자 프로필이 없으면 null입니다.")
    val authorNickname: String?,
    val registeredAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminConcernSummary): AdminConcernSummaryResponse = AdminConcernSummaryResponse(
            id = result.id,
            category = result.category,
            title = result.title,
            viewCount = result.viewCount,
            commentCount = result.commentCount,
            hasOfficialComment = result.hasOfficialComment,
            visibility = result.visibility,
            authorUserId = result.authorUserId,
            authorNickname = result.authorNickname,
            registeredAt = result.registeredAt,
        )
    }
}

data class AdminConcernDetailResponse(
    val id: Long,
    val category: ConcernCategory,
    val title: String,
    @Schema(description = "일반 텍스트 본문입니다.")
    val content: String,
    val viewCount: Long,
    @Schema(description = "남아 있는 답변 수입니다. 답글은 세지 않습니다.")
    val commentCount: Long,
    @Schema(description = "삭제되지 않은 운영자 답변이 있는지입니다.")
    val hasOfficialComment: Boolean,
    val visibility: AdminContentVisibility,
    val authorUserId: Long,
    @Schema(description = "작성자 프로필이 없으면 null입니다.")
    val authorNickname: String?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminConcernDetail): AdminConcernDetailResponse = AdminConcernDetailResponse(
            id = result.id,
            category = result.category,
            title = result.title,
            content = result.content,
            viewCount = result.viewCount,
            commentCount = result.commentCount,
            hasOfficialComment = result.hasOfficialComment,
            visibility = result.visibility,
            authorUserId = result.authorUserId,
            authorNickname = result.authorNickname,
            registeredAt = result.registeredAt,
            updatedAt = result.updatedAt,
        )
    }
}
