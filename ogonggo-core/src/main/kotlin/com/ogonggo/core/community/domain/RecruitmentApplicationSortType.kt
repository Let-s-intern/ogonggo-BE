package com.ogonggo.core.community.domain

import com.ogonggo.core.enumeration.EnumField

enum class RecruitmentApplicationSortType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    LATEST(1, "최근 지원순"),
}
