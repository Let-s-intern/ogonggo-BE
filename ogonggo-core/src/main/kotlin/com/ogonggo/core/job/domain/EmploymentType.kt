package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

enum class EmploymentType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    FULL_TIME(1, "정규직"),
    CONTRACT(2, "계약직"),
    INTERN(3, "인턴"),
    PART_TIME(4, "파트타임"),
    ETC(5, "기타"),
}
