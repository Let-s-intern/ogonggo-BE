package com.ogonggo.core.letscareercontent.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 렛츠커리어 콘텐츠의 종류다. 렛츠커리어 웹에서 상세 화면이 나뉘는 단위와 같다.
 * 프로그램 다섯 가지와 무료 자료집, 블로그다.
 */
enum class LetsCareerContentKind(
    override val code: Int,
    override val desc: String,
) : EnumField {
    CHALLENGE(1, "챌린지"),
    LIVE(2, "라이브"),
    VOD(3, "VOD"),
    GUIDEBOOK(4, "가이드북"),
    REPORT(5, "서류 진단"),
    MATERIAL(6, "무료 자료집"),
    BLOG(7, "블로그"),
}
