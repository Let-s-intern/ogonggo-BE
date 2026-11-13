package com.ogonggo.adminapi.announcement.presentation.response

import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.core.announcement.domain.AnnouncementPushTarget
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
    val category: AnnouncementCategory,
    val thumbnailUrl: String?,
    val summary: String?,
    @Schema(description = "노출할 때 푸시 알림을 보낼 대상. NONE이면 보내지 않습니다.")
    val pushTarget: AnnouncementPushTarget,
    @Schema(description = "보낸 푸시의 발송 묶음 식별자. 발송 결과 조회(`GET /api/v1/admin/notification-batches/{batchId}`)에 씁니다. 보내기 전에는 null입니다.")
    val pushBatchId: String?,
    @Schema(description = "푸시를 보낸 일시. 보내기 전에는 null입니다.")
    val pushedAt: LocalDateTime?,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminAnnouncementSummary): AdminAnnouncementSummaryResponse = AdminAnnouncementSummaryResponse(
            id = result.id,
            title = result.title,
            pinned = result.pinned,
            visibility = result.visibility,
            category = result.category,
            thumbnailUrl = result.thumbnailUrl,
            summary = result.summary,
            pushTarget = result.pushTarget,
            pushBatchId = result.pushBatchId,
            pushedAt = result.pushedAt,
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
    val category: AnnouncementCategory,
    val thumbnailUrl: String?,
    val summary: String?,
    @Schema(description = "노출할 때 푸시 알림을 보낼 대상. NONE이면 보내지 않습니다.")
    val pushTarget: AnnouncementPushTarget,
    @Schema(description = "보낸 푸시의 발송 묶음 식별자. 발송 결과 조회(`GET /api/v1/admin/notification-batches/{batchId}`)에 씁니다. 보내기 전에는 null입니다.")
    val pushBatchId: String?,
    @Schema(description = "푸시를 보낸 일시. 보내기 전에는 null입니다.")
    val pushedAt: LocalDateTime?,
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
                category = summary.category,
                thumbnailUrl = summary.thumbnailUrl,
                summary = summary.summary,
                pushTarget = summary.pushTarget,
                pushBatchId = summary.pushBatchId,
                pushedAt = summary.pushedAt,
                registeredAt = summary.registeredAt,
                updatedAt = summary.updatedAt,
            )
        }
    }
}
