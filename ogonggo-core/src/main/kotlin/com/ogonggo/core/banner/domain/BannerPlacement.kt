package com.ogonggo.core.banner.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 사용자 웹에서 배너가 나가는 자리다. 광고주별로 모아 볼 때 이 선언 순서로 자리를 늘어놓는다.
 *
 * 상세 사이드 자리(`JOB_DETAIL_SIDE`, `BOOTCAMP_DETAIL_SIDE`)는 PC 이미지가 세로 소재(400×500)라 좁은 화면에 쓸 수 없어
 * 모바일 이미지(750×200)가 꼭 있어야 한다. 목록 위·아래 자리는 모바일 이미지가 없으면 웹이 PC 이미지를 쓴다.
 */
enum class BannerPlacement(
    override val code: Int,
    override val desc: String,
    val requiresMobileImage: Boolean,
) : EnumField {
    HOME_TOP(1, "채용공고 홈 히어로 아래", false),
    HOME_BOTTOM(2, "채용공고 홈 목록 아래", false),
    JOB_DETAIL_SIDE(3, "공고 상세 사이드바", true),
    BOOTCAMP_TOP(4, "부트캠프 목록 위", false),
    BOOTCAMP_BOTTOM(5, "부트캠프 목록 아래", false),
    BOOTCAMP_DETAIL_SIDE(6, "부트캠프 상세 사이드바", true),
}
