package com.ogonggo.core.concern.domain

import com.ogonggo.core.enumeration.EnumField

enum class ConcernSortType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    LATEST(1, "최신순"),
    VIEW_COUNT(2, "조회 많은 순"),
    COMMENT_COUNT(3, "답변 많은 순"),
}
