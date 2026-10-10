package com.ogonggo.core.recruitmentpost.domain

import com.ogonggo.core.enumeration.EnumField

enum class RecruitmentPostApplicantPresence(
    override val code: Int,
    override val desc: String,
) : EnumField {
    HAS_APPLICATIONS(1, "지원 있음"),
    NO_APPLICATIONS(2, "지원 없음"),
}
