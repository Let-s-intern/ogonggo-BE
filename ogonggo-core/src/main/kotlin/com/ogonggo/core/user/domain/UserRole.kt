package com.ogonggo.core.user.domain

import com.ogonggo.core.enumeration.EnumField

enum class UserRole(
    override val code: Int,
    override val desc: String,
) : EnumField {
    USER(1, "일반 회원"),
    COMPANY(2, "기업 회원"),
    ADMIN(3, "관리자 회원"),
    MENTOR(4, "멘토 회원"),
    ;

    companion object {
        /** 렛츠커리어 계정으로 만든 일반 회원 계정의 역할이다. 역할만 다르고 일반 회원 기능을 똑같이 쓴다. */
        val GENERAL_MEMBER_ROLES: Set<UserRole> = setOf(USER, MENTOR, ADMIN)
    }
}
