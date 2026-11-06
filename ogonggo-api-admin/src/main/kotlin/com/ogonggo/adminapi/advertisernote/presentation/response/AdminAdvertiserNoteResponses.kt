package com.ogonggo.adminapi.advertisernote.presentation.response

import com.ogonggo.adminapi.advertisernote.business.AdminAdvertiserNoteResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminAdvertiserNoteResponse(
    val id: Long,
    val advertiserId: Long,
    val title: String,
    val content: String,
    @Schema(description = "쓴 관리자 이름(관리자 토큰의 회원 프로필 이름). 모르면 null입니다.")
    val authorName: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(result: AdminAdvertiserNoteResult): AdminAdvertiserNoteResponse = AdminAdvertiserNoteResponse(
            id = result.id,
            advertiserId = result.advertiserId,
            title = result.title,
            content = result.content,
            authorName = result.authorName,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
        )
    }
}

/** 지운 메모의 식별자다. */
data class AdminAdvertiserNoteIdResponse(
    val id: Long,
)
