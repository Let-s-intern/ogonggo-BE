package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

enum class JobEmploymentType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    FULL_TIME(1, "정규직"),
    CONTRACT(2, "계약직"),
    INTERN(3, "인턴"),
    PART_TIME(4, "파트타임"),
    WORK_STUDY(6, "일학습병행"),
    WORK_EXPERIENCE(7, "미래내일 일경험"),
    ETC(5, "기타"),
}
