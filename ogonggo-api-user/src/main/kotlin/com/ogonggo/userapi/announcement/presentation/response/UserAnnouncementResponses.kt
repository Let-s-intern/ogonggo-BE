package com.ogonggo.userapi.announcement.presentation.response

import com.ogonggo.userapi.announcement.business.UserAnnouncementResult
import com.ogonggo.userapi.announcement.business.UserAnnouncementSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserAnnouncementSummaryResponse(
    val id: Long,
    val title: String,
    val pinned: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: UserAnnouncementSummary): UserAnnouncementSummaryResponse = UserAnnouncementSummaryResponse(
            id = result.id,
            title = result.title,
            pinned = result.pinned,
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
    val createdAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: UserAnnouncementResult): UserAnnouncementDetailResponse = UserAnnouncementDetailResponse(
            id = result.summary.id,
            title = result.summary.title,
            content = result.content,
            pinned = result.summary.pinned,
            createdAt = result.summary.createdAt,
        )
    }
}
