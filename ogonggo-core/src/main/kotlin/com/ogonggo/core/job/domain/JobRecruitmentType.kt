package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

enum class JobRecruitmentType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    PERIOD(1, "기간 채용"),
    ALWAYS_OPEN(2, "상시 채용"),
}
