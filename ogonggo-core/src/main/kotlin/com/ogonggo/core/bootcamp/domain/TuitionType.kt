package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.enumeration.EnumField

enum class TuitionType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    FREE(1, "무료"),
    PAID(2, "유료"),
    GOVERNMENT_FUNDED(3, "국비 지원"),
}
