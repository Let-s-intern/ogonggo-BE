package com.ogonggo.core.concern.domain

import com.ogonggo.core.enumeration.EnumField

/** "지금 가장 핫한 고민"을 고르는 기준이다. */
enum class ConcernPopularSortType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    VIEW_COUNT(1, "조회 많은 순"),
    COMMENT_COUNT(2, "답변 많은 순"),
}
