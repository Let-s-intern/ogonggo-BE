package com.ogonggo.userapi.announcement.business

import com.ogonggo.core.announcement.domain.Announcement
import com.ogonggo.core.announcement.implement.dto.AnnouncementPageDto
import java.time.LocalDateTime

data class UserAnnouncementPageResult(
    val items: List<UserAnnouncementSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(result: AnnouncementPageDto): UserAnnouncementPageResult = UserAnnouncementPageResult(
            items = result.announcements.map(UserAnnouncementSummary::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class UserAnnouncementSummary(
    val id: Long,
    val title: String,
    val pinned: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        internal fun from(announcement: Announcement): UserAnnouncementSummary = UserAnnouncementSummary(
            id = announcement.requiredId(),
            title = announcement.title,
            pinned = announcement.pinned,
            createdAt = announcement.createdAt,
        )
    }
}

data class UserAnnouncementResult(
    val summary: UserAnnouncementSummary,
    val content: String,
) {
    companion object {
        internal fun from(announcement: Announcement): UserAnnouncementResult = UserAnnouncementResult(
            summary = UserAnnouncementSummary.from(announcement),
            content = announcement.content,
        )
    }
}

private fun Announcement.requiredId(): Long = checkNotNull(id) { "공지 식별자가 없습니다." }
