package com.ogonggo.userapi.announcement.presentation.response

import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.userapi.announcement.business.UserAnnouncementCategoryResult
import com.ogonggo.userapi.announcement.business.UserAnnouncementResult
import com.ogonggo.userapi.announcement.business.UserAnnouncementSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

private const val THUMBNAIL_DESCRIPTION = "추천 카드 썸네일 주소. 없으면 null이며 화면은 카테고리 기본 이미지를 보입니다."
private const val SUMMARY_DESCRIPTION = "목록·추천 카드에 보이는 한두 줄 요약. 없으면 null입니다."

data class UserAnnouncementSummaryResponse(
    val id: Long,
    val title: String,
    @Schema(description = "상단 고정 공지입니다. 공지사항 화면은 고정 공지를 맨 위 추천 카드로 보입니다.")
    val pinned: Boolean,
    val category: AnnouncementCategory,
    @Schema(description = THUMBNAIL_DESCRIPTION)
    val thumbnailUrl: String?,
    @Schema(description = SUMMARY_DESCRIPTION)
    val summary: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: UserAnnouncementSummary): UserAnnouncementSummaryResponse = UserAnnouncementSummaryResponse(
            id = result.id,
            title = result.title,
            pinned = result.pinned,
            category = result.category,
            thumbnailUrl = result.thumbnailUrl,
            summary = result.summary,
            createdAt = result.createdAt,
        )
    }
}

data class UserAnnouncementDetailResponse(
    val id: Long,
    val title: String,
    @Schema(description = "Lexical EditorState JSON 문자열입니다.")
    val content: String,
    val pinned: Boolean,
    val category: AnnouncementCategory,
    @Schema(description = THUMBNAIL_DESCRIPTION)
    val thumbnailUrl: String?,
    @Schema(description = SUMMARY_DESCRIPTION)
    val summary: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: UserAnnouncementResult): UserAnnouncementDetailResponse = UserAnnouncementDetailResponse(
            id = result.summary.id,
            title = result.summary.title,
            content = result.content,
            pinned = result.summary.pinned,
            category = result.summary.category,
            thumbnailUrl = result.summary.thumbnailUrl,
            summary = result.summary.summary,
            createdAt = result.summary.createdAt,
        )
    }
}

data class UserAnnouncementCategoryResponse(
    val category: AnnouncementCategory,
    @Schema(description = "이 카테고리의 노출 중인 공지 수입니다.")
    val count: Long,
    @Schema(description = "이 카테고리의 최근 공지 3건(최신순, 고정 여부와 상관없음). 없으면 빈 목록입니다.")
    val recentAnnouncements: List<UserAnnouncementSummaryResponse>,
) {
    companion object {
        internal fun from(result: UserAnnouncementCategoryResult): UserAnnouncementCategoryResponse = UserAnnouncementCategoryResponse(
            category = result.category,
            count = result.count,
            recentAnnouncements = result.recentAnnouncements.map(UserAnnouncementSummaryResponse::from),
        )
    }
}
