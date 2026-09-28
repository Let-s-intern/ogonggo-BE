package com.ogonggo.adminapi.notice.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.notice.domain.Notice
import com.ogonggo.core.notice.implement.dto.NoticePageDto
import java.time.LocalDateTime

data class AdminNoticePageResult(
    val items: List<AdminNoticeSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(result: NoticePageDto): AdminNoticePageResult = AdminNoticePageResult(
            items = result.notices.map(AdminNoticeSummary::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class AdminNoticeSummary(
    val id: Long,
    val title: String,
    val pinned: Boolean,
    val visibility: AdminContentVisibility,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(notice: Notice): AdminNoticeSummary = AdminNoticeSummary(
            id = notice.requiredId(),
            title = notice.title,
            pinned = notice.pinned,
            visibility = AdminContentVisibility.of(notice.published),
            registeredAt = notice.createdAt,
            updatedAt = notice.updatedAt,
        )
    }
}

data class AdminNoticeResult(
    val summary: AdminNoticeSummary,
    val content: String,
) {
    companion object {
        internal fun from(notice: Notice): AdminNoticeResult = AdminNoticeResult(
            summary = AdminNoticeSummary.from(notice),
            content = notice.content,
        )
    }
}

internal fun Notice.requiredId(): Long = checkNotNull(id) { "공지 식별자가 없습니다." }
