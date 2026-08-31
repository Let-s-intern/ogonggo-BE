package com.ogonggo.core.job.domain

import com.ogonggo.core.enumeration.EnumField

enum class ExperienceType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    NEWCOMER(1, "신입"),
    EXPERIENCED(2, "경력"),
    BOTH(3, "신입·경력"),
    IRRELEVANT(4, "경력 무관"),
}
