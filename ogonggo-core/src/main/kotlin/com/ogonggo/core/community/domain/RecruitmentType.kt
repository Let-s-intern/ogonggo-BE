package com.ogonggo.core.community.domain

import com.ogonggo.core.enumeration.EnumField

enum class RecruitmentType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    SIDE_PROJECT(1, "사이드 프로젝트"),
    STUDY(2, "스터디"),
}
