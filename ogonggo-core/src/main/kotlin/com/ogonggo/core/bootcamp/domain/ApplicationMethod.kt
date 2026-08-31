package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.enumeration.EnumField

enum class ApplicationMethod(
    override val code: Int,
    override val desc: String,
) : EnumField {
    EXTERNAL_PAGE(1, "외부 페이지"),
    EMAIL(2, "이메일"),
}
