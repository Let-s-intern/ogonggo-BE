package com.ogonggo.adminapi.advertisernote.business

import com.ogonggo.core.advertisernote.domain.AdvertiserNote
import java.time.LocalDateTime

data class AdminAdvertiserNoteResult(
    val id: Long,
    val advertiserId: Long,
    val title: String,
    val content: String,
    /** 쓴 관리자의 프로필 이름. 쓴 사람을 모르거나 프로필에 이름이 없으면 null이다. */
    val authorName: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun of(note: AdvertiserNote, authorName: String?): AdminAdvertiserNoteResult = AdminAdvertiserNoteResult(
            id = note.requiredId(),
            advertiserId = note.advertiserId,
            title = note.title,
            content = note.content,
            authorName = authorName,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
        )
    }
}

internal fun AdvertiserNote.requiredId(): Long = checkNotNull(id) { "광고주 메모 식별자가 없습니다." }
