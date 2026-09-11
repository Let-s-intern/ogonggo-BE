package com.ogonggo.core.community.domain

import com.ogonggo.core.enumeration.EnumField

enum class RecruitmentPostSortType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    LATEST(1, "최신순"),
    DEADLINE(2, "마감 임박순"),
}
