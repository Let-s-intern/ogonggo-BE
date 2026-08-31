package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.enumeration.EnumField

enum class BootcampStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    DRAFT(1, "임시저장"),
    RECRUITING(2, "모집중"),
    CLOSED(3, "모집 마감"),
}
