package com.ogonggo.core.announcement.implement.dto

import com.ogonggo.core.announcement.domain.Announcement

data class AnnouncementAppendDto(
    val title: String,
    /** 검증과 정규화를 마친 Lexical EditorState JSON이다. */
    val content: String,
    val pinned: Boolean,
    val published: Boolean,
)

/** null인 값은 바꾸지 않는다. */
data class AnnouncementUpdateDto(
    val title: String? = null,
    /** 검증과 정규화를 마친 Lexical EditorState JSON이다. */
    val content: String? = null,
    val pinned: Boolean? = null,
    val published: Boolean? = null,
)

data class AnnouncementPageDto(
    val announcements: List<Announcement>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
