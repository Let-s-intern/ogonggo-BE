package com.ogonggo.core.user.domain

import com.ogonggo.core.enumeration.EnumField

enum class UserStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    ACTIVE(1, "활성"),
    WITHDRAWN(2, "탈퇴"),
    SUSPENDED(3, "정지"),
}
