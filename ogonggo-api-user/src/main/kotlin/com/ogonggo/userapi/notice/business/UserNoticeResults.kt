package com.ogonggo.userapi.notice.business

import com.ogonggo.core.notice.domain.Notice
import com.ogonggo.core.notice.implement.dto.NoticePageDto
import java.time.LocalDateTime

data class UserNoticePageResult(
    val items: List<UserNoticeSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        internal fun from(result: NoticePageDto): UserNoticePageResult = UserNoticePageResult(
            items = result.notices.map(UserNoticeSummary::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class UserNoticeSummary(
    val id: Long,
    val title: String,
    val pinned: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        internal fun from(notice: Notice): UserNoticeSummary = UserNoticeSummary(
            id = notice.requiredId(),
            title = notice.title,
            pinned = notice.pinned,
            createdAt = notice.createdAt,
        )
    }
}

data class UserNoticeResult(
    val summary: UserNoticeSummary,
    val content: String,
) {
    companion object {
        internal fun from(notice: Notice): UserNoticeResult = UserNoticeResult(
            summary = UserNoticeSummary.from(notice),
            content = notice.content,
        )
    }
}

private fun Notice.requiredId(): Long = checkNotNull(id) { "공지 식별자가 없습니다." }
