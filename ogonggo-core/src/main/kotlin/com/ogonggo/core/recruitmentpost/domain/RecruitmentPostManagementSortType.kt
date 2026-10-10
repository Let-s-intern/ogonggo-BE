package com.ogonggo.core.recruitmentpost.domain

import com.ogonggo.core.enumeration.EnumField

enum class RecruitmentPostManagementSortType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    LATEST_SAVED(1, "최근 저장순"),
}
