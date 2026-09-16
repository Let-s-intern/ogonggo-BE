package com.ogonggo.core.community.domain

import com.ogonggo.core.enumeration.EnumField

enum class RecruitmentPosition(
    override val code: Int,
    override val desc: String,
) : EnumField {
    BACKEND(1, "백엔드"),
    FRONTEND(2, "프론트엔드"),
    DESIGN(3, "디자인"),
    PM(4, "기획"),
    MOBILE(5, "모바일"),
    ETC(6, "기타"),
}
