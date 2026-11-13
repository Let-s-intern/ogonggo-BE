package com.ogonggo.adminapi.announcement.business

import com.ogonggo.core.announcement.domain.AnnouncementCategory
import com.ogonggo.core.announcement.domain.AnnouncementPushTarget
import com.ogonggo.adminapi.content.business.AdminContentVisibility

data class AdminAnnouncementCreateCommand(
    val title: String,
    /** Lexical EditorState JSON이다. */
    val content: String,
    val pinned: Boolean,
    val visibility: AdminContentVisibility,
    val category: AnnouncementCategory,
    val thumbnailUrl: String?,
    val summary: String?,
    val pushTarget: AnnouncementPushTarget = AnnouncementPushTarget.NONE,
)

/** 넘어온 값만 바꾼다. */
data class AdminAnnouncementUpdateCommand(
    val title: String? = null,
    /** Lexical EditorState JSON이다. */
    val content: String? = null,
    val pinned: Boolean? = null,
    val visibility: AdminContentVisibility? = null,
    val category: AnnouncementCategory? = null,
    /** 빈 글자면 지운다. */
    val thumbnailUrl: String? = null,
    /** 빈 글자면 지운다. */
    val summary: String? = null,
    val pushTarget: AnnouncementPushTarget? = null,
)
