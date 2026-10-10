package com.ogonggo.adminapi.announcement.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.announcement.domain.Announcement
import com.ogonggo.core.announcement.implement.dto.AnnouncementPageDto
import java.time.LocalDateTime

data class AdminAnnouncementPageResult(
    val items: List<AdminAnnouncementSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(result: AnnouncementPageDto): AdminAnnouncementPageResult = AdminAnnouncementPageResult(
            items = result.announcements.map(AdminAnnouncementSummary::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class AdminAnnouncementSummary(
    val id: Long,
    val title: String,
    val pinned: Boolean,
    val visibility: AdminContentVisibility,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(announcement: Announcement): AdminAnnouncementSummary = AdminAnnouncementSummary(
            id = announcement.requiredId(),
            title = announcement.title,
            pinned = announcement.pinned,
            visibility = AdminContentVisibility.of(announcement.published),
            registeredAt = announcement.createdAt,
            updatedAt = announcement.updatedAt,
        )
    }
}

data class AdminAnnouncementResult(
    val summary: AdminAnnouncementSummary,
    val content: String,
) {
    companion object {
        internal fun from(announcement: Announcement): AdminAnnouncementResult = AdminAnnouncementResult(
            summary = AdminAnnouncementSummary.from(announcement),
            content = announcement.content,
        )
    }
}

internal fun Announcement.requiredId(): Long = checkNotNull(id) { "공지 식별자가 없습니다." }
