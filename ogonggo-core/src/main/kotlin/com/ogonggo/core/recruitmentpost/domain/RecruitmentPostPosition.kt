package com.ogonggo.core.recruitmentpost.domain

import com.ogonggo.core.enumeration.EnumField

enum class RecruitmentPostPosition(
    override val code: Int,
    override val desc: String,
) : EnumField {
    BACKEND(1, "백엔드"),
    FRONTEND(2, "프론트엔드"),
    DESIGN(3, "디자인"),
    PM(4, "기획"),
    MARKETING(7, "마케팅"),
    ETC(6, "기타"),

    /**
     * 선택지에서 빠진 값이다. 프런트가 마케팅으로 전환하는 동안 이전 화면의 요청과 기존 모집글을 받기 위해 남겨 둔다.
     * 서버 2차 배포에서 저장된 값을 MARKETING으로 옮기고 제거한다(`docs/schema/2026-10-06-recruitment-post-position-mobile-cleanup.sql`).
     */
    MOBILE(5, "모바일"),
}
