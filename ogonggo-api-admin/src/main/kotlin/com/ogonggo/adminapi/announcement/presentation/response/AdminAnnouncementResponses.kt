package com.ogonggo.adminapi.announcement.presentation.response

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.announcement.business.AdminAnnouncementResult
import com.ogonggo.adminapi.announcement.business.AdminAnnouncementSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminAnnouncementSummaryResponse(
    val id: Long,
    val title: String,
    val pinned: Boolean,
    val visibility: AdminContentVisibility,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminAnnouncementSummary): AdminAnnouncementSummaryResponse = AdminAnnouncementSummaryResponse(
            id = result.id,
            title = result.title,
            pinned = result.pinned,
            visibility = result.visibility,
            registeredAt = result.registeredAt,
            updatedAt = result.updatedAt,
        )
    }
}

data class AdminAnnouncementDetailResponse(
    val id: Long,
    val title: String,
    @Schema(description = "Lexical EditorState JSON 문자열입니다.")
    val content: String,
    val pinned: Boolean,
    val visibility: AdminContentVisibility,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminAnnouncementResult): AdminAnnouncementDetailResponse {
            val summary = result.summary
            return AdminAnnouncementDetailResponse(
                id = summary.id,
                title = summary.title,
                content = result.content,
                pinned = summary.pinned,
                visibility = summary.visibility,
                registeredAt = summary.registeredAt,
                updatedAt = summary.updatedAt,
            )
        }
    }
}
