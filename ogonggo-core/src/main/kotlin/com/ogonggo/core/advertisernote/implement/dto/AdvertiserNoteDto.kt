package com.ogonggo.core.advertisernote.implement.dto

data class AdvertiserNoteAppendDto(
    val advertiserId: Long,
    val title: String,
    val content: String,
    val authorUserId: Long?,
)

/** 넘어온 칸만 바꾼다. */
data class AdvertiserNoteUpdateDto(
    val title: String? = null,
    val content: String? = null,
)
