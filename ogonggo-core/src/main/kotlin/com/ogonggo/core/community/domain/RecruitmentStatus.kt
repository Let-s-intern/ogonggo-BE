package com.ogonggo.core.community.domain

import com.ogonggo.core.enumeration.EnumField

enum class RecruitmentStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    RECRUITING(1, "모집 중"),
    CLOSED(2, "마감"),
}
