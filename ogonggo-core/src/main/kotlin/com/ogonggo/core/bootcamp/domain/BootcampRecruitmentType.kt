package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.enumeration.EnumField

enum class BootcampRecruitmentType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    PERIOD(1, "기간 모집"),
    ALWAYS_OPEN(2, "상시 모집"),
}
