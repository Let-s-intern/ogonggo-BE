package com.ogonggo.adminapi.member.presentation.request

import com.ogonggo.adminapi.error.InvalidRequestFieldException
import com.ogonggo.core.user.domain.UserRole

data class ChangeGeneralMemberRoleRequest(
    val role: UserRole,
) {
    /** 기업 회원은 가입 방식이 달라 일반 회원을 기업 회원으로 바꿀 수 없다. */
    fun generalMemberRole(): UserRole {
        if (role !in UserRole.GENERAL_MEMBER_ROLES) {
            throw InvalidRequestFieldException("role", "일반 회원 역할(USER·MENTOR·ADMIN)로만 바꿀 수 있습니다.")
        }
        return role
    }
}
