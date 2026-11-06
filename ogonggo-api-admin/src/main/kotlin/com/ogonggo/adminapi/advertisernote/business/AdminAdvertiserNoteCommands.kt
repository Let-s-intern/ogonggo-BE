package com.ogonggo.adminapi.advertisernote.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.advertisernote.domain.AdvertiserNote

/** 메모 등록 본문이다. 검증 전 값을 그대로 담는다. */
data class AdminAdvertiserNoteCreateCommand(
    val title: String?,
    val content: String?,
)

/** 메모 수정 본문이다. null이면 그대로 둔다. */
data class AdminAdvertiserNoteUpdateCommand(
    val title: String? = null,
    val content: String? = null,
)

/** 앞뒤 공백을 뺀 제목이다. 1~100자가 아니면 화면 문구로 알린다. */
internal fun validNoteTitle(title: String?): String = validText("title", title, AdvertiserNote.MAX_TITLE_LENGTH)

/** 앞뒤 공백을 뺀 내용이다. 1~2000자가 아니면 화면 문구로 알린다. */
internal fun validNoteContent(content: String?): String = validText("content", content, AdvertiserNote.MAX_CONTENT_LENGTH)

private fun validText(fieldName: String, value: String?, max: Int): String {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isEmpty() || trimmed.length > max) {
        throw AdminConsoleBadRequestException.field(fieldName, "1~${max}자로 보내 주세요.")
    }
    return trimmed
}
