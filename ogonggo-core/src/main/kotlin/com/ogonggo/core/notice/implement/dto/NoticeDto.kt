package com.ogonggo.core.notice.implement.dto

import com.ogonggo.core.notice.domain.Notice

data class NoticeAppendDto(
    val title: String,
    /** 검증과 정규화를 마친 Lexical EditorState JSON이다. */
    val content: String,
    val pinned: Boolean,
    val published: Boolean,
)

/** null인 값은 바꾸지 않는다. */
data class NoticeUpdateDto(
    val title: String? = null,
    /** 검증과 정규화를 마친 Lexical EditorState JSON이다. */
    val content: String? = null,
    val pinned: Boolean? = null,
    val published: Boolean? = null,
)

data class NoticePageDto(
    val notices: List<Notice>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
