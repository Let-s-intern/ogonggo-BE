package com.ogonggo.core.user.domain

import com.ogonggo.core.enumeration.EnumField

enum class UserRole(
    override val code: Int,
    override val desc: String,
) : EnumField {
    USER(1, "일반 회원"),
    COMPANY(2, "기업 회원"),
    ADMIN(3, "관리자 회원"),
}
