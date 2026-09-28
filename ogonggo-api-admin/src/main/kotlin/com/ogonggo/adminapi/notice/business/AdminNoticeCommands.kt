package com.ogonggo.adminapi.notice.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility

data class AdminNoticeCreateCommand(
    val title: String,
    /** Lexical EditorState JSON이다. */
    val content: String,
    val pinned: Boolean,
    val visibility: AdminContentVisibility,
)

/** 넘어온 값만 바꾼다. */
data class AdminNoticeUpdateCommand(
    val title: String? = null,
    /** Lexical EditorState JSON이다. */
    val content: String? = null,
    val pinned: Boolean? = null,
    val visibility: AdminContentVisibility? = null,
)
