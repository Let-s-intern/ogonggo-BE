package com.ogonggo.userapi.notice.presentation.response

import com.ogonggo.userapi.notice.business.UserNoticeResult
import com.ogonggo.userapi.notice.business.UserNoticeSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserNoticeSummaryResponse(
    val id: Long,
    val title: String,
    val pinned: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: UserNoticeSummary): UserNoticeSummaryResponse = UserNoticeSummaryResponse(
            id = result.id,
            title = result.title,
            pinned = result.pinned,
            createdAt = result.createdAt,
        )
    }
}

data class UserNoticeDetailResponse(
    val id: Long,
    val title: String,
    @Schema(description = "Lexical EditorState JSON 문자열입니다.")
    val content: String,
    val pinned: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: UserNoticeResult): UserNoticeDetailResponse = UserNoticeDetailResponse(
            id = result.summary.id,
            title = result.summary.title,
            content = result.content,
            pinned = result.summary.pinned,
            createdAt = result.summary.createdAt,
        )
    }
}
