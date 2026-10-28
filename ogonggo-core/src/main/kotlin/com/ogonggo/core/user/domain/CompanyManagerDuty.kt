package com.ogonggo.core.user.domain

import com.ogonggo.core.enumeration.EnumField

/** 기업 회원 담당자가 맡은 업무다. 직책(자유 입력)과 따로 고른다. */
enum class CompanyManagerDuty(
    override val code: Int,
    override val desc: String,
) : EnumField {
    HR(1, "인사·채용"),
    EXECUTIVE(2, "대표·경영"),
    MARKETING(3, "마케팅·홍보"),
    OTHER(4, "기타"),
}
