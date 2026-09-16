package com.ogonggo.userapi.response

data class CursorPageResponse<T>(
    val items: List<T>,
    val hasNext: Boolean,
    val nextCursor: String?,
)
