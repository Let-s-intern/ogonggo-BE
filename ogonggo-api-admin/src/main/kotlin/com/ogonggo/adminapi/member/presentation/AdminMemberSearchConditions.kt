package com.ogonggo.adminapi.member.presentation

import com.ogonggo.adminapi.error.InvalidRequestParameterException
import com.ogonggo.core.user.domain.UserManagementSearchCondition
import com.ogonggo.core.user.domain.UserStatus
import java.time.LocalDate

/** 일반·기업 회원 목록이 같은 필터를 쓴다. 검색어가 가리키는 칸만 목록마다 다르다. */
internal fun memberSearchCondition(
    keyword: String?,
    status: UserStatus?,
    joinedFrom: LocalDate?,
    joinedTo: LocalDate?,
): UserManagementSearchCondition {
    if (joinedFrom != null && joinedTo != null && joinedFrom.isAfter(joinedTo)) {
        throw InvalidRequestParameterException("joinedFrom", "가입 기간 시작일은 종료일보다 늦을 수 없습니다.")
    }
    return UserManagementSearchCondition(
        status = status,
        joinedFrom = joinedFrom,
        joinedTo = joinedTo,
        keyword = keyword?.takeIf { it.isNotBlank() },
    )
}
