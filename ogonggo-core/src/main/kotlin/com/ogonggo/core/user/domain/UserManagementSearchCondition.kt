package com.ogonggo.core.user.domain

import java.time.LocalDate

/** 관리자 콘솔 회원 목록의 선택 조건이다. null인 조건은 적용하지 않는다. */
data class UserManagementSearchCondition(
    val status: UserStatus? = null,
    /** 이 날짜 0시부터 가입한 회원을 찾는다. */
    val joinedFrom: LocalDate? = null,
    /** 이 날짜가 끝날 때까지 가입한 회원을 찾는다. 당일을 포함한다. */
    val joinedTo: LocalDate? = null,
    /**
     * 대소문자를 가리지 않고 부분 일치로 찾는다.
     * 일반 회원은 이름·닉네임·이메일, 기업 회원은 회사명·담당자 이름·계정 이메일에서 찾는다.
     */
    val keyword: String? = null,
    /** 일반 회원 목록에서만 쓴다. 기업 회원 목록은 역할이 하나뿐이다. */
    val role: UserRole? = null,
    /** 일반 회원 목록에서만 쓴다. 아직 정하지 못한 회원은 어느 값으로 걸러도 나오지 않는다. */
    val inflowSource: UserInflowSource? = null,
)
