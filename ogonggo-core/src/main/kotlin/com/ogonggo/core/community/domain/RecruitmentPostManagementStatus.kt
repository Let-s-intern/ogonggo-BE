package com.ogonggo.core.community.domain

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

enum class RecruitmentPostApplicationStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    HAS_APPLICATIONS(1, "지원 있음"),
    NO_APPLICATIONS(2, "지원 없음"),
}

enum class RecruitmentPostManagementSortType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    LATEST_SAVED(1, "최근 저장순"),
}
