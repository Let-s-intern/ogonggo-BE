package com.ogonggo.core.adposting.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 광고 공고를 싣는 사용자 웹의 목록 자리다. 자리가 대상의 종류를 정한다. 부트캠프 목록만 부트캠프이고 나머지는 채용공고다.
 * 광고주별 조회에서 자리를 가로질러 늘어놓을 때 이 선언 순서를 쓴다.
 */
enum class AdPostingPlacement(
    override val code: Int,
    override val desc: String,
    val targetType: AdPostingTargetType,
) : EnumField {
    JOB_LIST(1, "채용공고 홈 전체 공고", AdPostingTargetType.JOB),
    JOB_PICK(2, "홈 오공고 PICK", AdPostingTargetType.JOB),
    BOOTCAMP_LIST(3, "교육·부트캠프 목록", AdPostingTargetType.BOOTCAMP),
    JOB_DETAIL_SIDE(4, "공고 상세 비슷한 공고", AdPostingTargetType.JOB),
    CALENDAR(5, "공고 달력 날짜별 목록", AdPostingTargetType.JOB),
    MYPAGE_SCRAP(6, "마이페이지 지원 보드 스크랩", AdPostingTargetType.JOB),
}
