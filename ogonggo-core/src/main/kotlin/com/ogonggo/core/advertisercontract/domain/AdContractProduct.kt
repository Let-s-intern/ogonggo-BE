package com.ogonggo.core.advertisercontract.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 광고 계약으로 산 자리다. 배너 자리와 광고 공고 자리에 같은 이름(`JOB_DETAIL_SIDE`)이 있어 종류를 앞에 붙인다.
 * 계약은 이 선언 순서로 정렬해 저장한다.
 *
 * 이름은 프런트 계약(`AdContractProduct`)을 따른다. 루트 이름(`AdvertiserContract`)을 앞에 붙이는 규칙과 다르다.
 */
enum class AdContractProduct(
    override val code: Int,
    override val desc: String,
) : EnumField {
    BANNER_HOME_TOP(1, "배너 채용공고 홈 상단"),
    BANNER_HOME_BOTTOM(2, "배너 채용공고 홈 하단"),
    BANNER_JOB_DETAIL_SIDE(3, "배너 공고 상세"),
    BANNER_BOOTCAMP_TOP(4, "배너 부트캠프 목록 상단"),
    BANNER_BOOTCAMP_BOTTOM(5, "배너 부트캠프 목록 하단"),
    BANNER_BOOTCAMP_DETAIL_SIDE(6, "배너 부트캠프 상세"),
    AD_POSTING_JOB_LIST(7, "광고 공고 채용공고 목록"),
    AD_POSTING_JOB_PICK(8, "광고 공고 오공고 PICK"),
    AD_POSTING_BOOTCAMP_LIST(9, "광고 공고 부트캠프 목록"),
    AD_POSTING_JOB_DETAIL_SIDE(10, "광고 공고 공고 상세"),
    AD_POSTING_CALENDAR(11, "광고 공고 공고 달력"),
    AD_POSTING_MYPAGE_SCRAP(12, "광고 공고 마이페이지 스크랩"),
}
