package com.ogonggo.core.announcement.implement.dto

import com.ogonggo.core.announcement.domain.Announcement
import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.core.announcement.domain.AnnouncementPushTarget

data class AnnouncementAppendDto(
    val title: String,
    /** 검증과 정규화를 마친 Lexical EditorState JSON이다. */
    val content: String,
    val pinned: Boolean,
    val published: Boolean,
    val category: AnnouncementCategory,
    val thumbnailUrl: String? = null,
    val summary: String? = null,
    val pushTarget: AnnouncementPushTarget = AnnouncementPushTarget.NONE,
)

/** null인 값은 바꾸지 않는다. */
data class AnnouncementUpdateDto(
    val title: String? = null,
    /** 검증과 정규화를 마친 Lexical EditorState JSON이다. */
    val content: String? = null,
    val pinned: Boolean? = null,
    val published: Boolean? = null,
    val category: AnnouncementCategory? = null,
    /** 빈 글자면 지운다. */
    val thumbnailUrl: String? = null,
    /** 빈 글자면 지운다. */
    val summary: String? = null,
    val pushTarget: AnnouncementPushTarget? = null,
)

/** 사용자 공지사항 화면의 카테고리 칸 하나. 노출 중인 공지 수와 최근 공지 몇 건이다. */
data class AnnouncementCategoryDigestDto(
    val category: AnnouncementCategory,
    val count: Long,
    val recentAnnouncements: List<Announcement>,
)

data class AnnouncementPageDto(
    val announcements: List<Announcement>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
