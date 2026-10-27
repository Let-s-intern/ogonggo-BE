package com.ogonggo.core.advertisementinquiry.domain

import com.ogonggo.core.enumeration.EnumField
import com.ogonggo.core.user.domain.UserRole

/**
 * 문의를 남긴 사람이 오공고 회원이었는지다. 저장할 때 서버가 토큰으로 정하고 클라이언트가 보낸 값은 받지 않는다.
 * 문의 시점의 구분이라 나중에 회원의 역할이 바뀌어도 다시 계산하지 않는다.
 */
enum class AdvertisementInquiryMemberType(
    override val code: Int,
    override val desc: String,
) : EnumField {
    COMPANY(1, "기업 회원"),
    USER(2, "일반 회원"),
    GUEST(3, "비회원"),
    ;

    companion object {
        /** 문의한 회원의 현재 역할로 정한다. 멘토·관리자도 일반 회원 계정이라 일반 회원이다. 회원이 아니면 비회원이다. */
        fun of(role: UserRole?): AdvertisementInquiryMemberType = when (role) {
            null -> GUEST
            UserRole.COMPANY -> COMPANY
            UserRole.USER, UserRole.MENTOR, UserRole.ADMIN -> USER
        }
    }
}
