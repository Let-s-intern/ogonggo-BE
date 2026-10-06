package com.ogonggo.core.recruitmentpost.domain

import com.ogonggo.core.enumeration.EnumField

enum class RecruitmentPostContactMethod(
    override val code: Int,
    override val desc: String,
) : EnumField {
    OPEN_KAKAO(1, "카카오톡 오픈채팅"),
    EMAIL(2, "이메일"),
}
