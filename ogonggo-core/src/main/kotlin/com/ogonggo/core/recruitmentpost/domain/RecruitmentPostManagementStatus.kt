package com.ogonggo.core.recruitmentpost.domain

import com.ogonggo.core.enumeration.EnumField

enum class RecruitmentPostManagementStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    ALL(0, "전체"),
    DRAFT(1, "임시저장"),
    PUBLISHED(2, "공개"),
    HIDDEN(3, "비공개"),
}
