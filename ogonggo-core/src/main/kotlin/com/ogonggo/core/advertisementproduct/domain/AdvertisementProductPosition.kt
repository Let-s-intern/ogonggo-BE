package com.ogonggo.core.advertisementproduct.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 광고가 노출되는 오공고 화면의 자리다. 프론트가 자기 자리의 광고를 찾는 고정 이름이다.
 *
 * 광고 상품은 어드민이 추가하는 행이라 식별자가 환경마다 달라 프론트가 붙잡을 수 없다.
 * 새 자리는 프론트가 그 자리를 그려야 생기므로 값을 더하는 배포와 함께 늘린다.
 * 광고 공고 자리는 판매 상품과 하나씩 맞춘다(2026-11-19, LC-3649): 홈 히어로 오공고 PICK, 채용공고 목록 직군 탭 맨 위,
 * 공고 상세 옆 추천, 공고 달력 날짜별 목록 맨 앞, 마이페이지 지원 관리 보드의 채용공고 스크랩 칸 맨 앞. 모두 채용공고를 띄운다.
 * 부트캠프 목록(8)은 LC-3649에서 뺐다가 판매 상품(부트캠프 목록 상단 고정)이 생겨 같은 코드 값으로 되살렸다(LC-3650). 이 자리만 부트캠프를 띄운다.
 */
enum class AdvertisementProductPosition(
    override val code: Int,
    override val desc: String,
    val category: AdvertisementProductCategory,
) : EnumField {
    JOB_HOME_TOP_BANNER(1, "채용공고 홈 상단", AdvertisementProductCategory.BANNER),
    JOB_HOME_BOTTOM_BANNER(2, "채용공고 홈 하단", AdvertisementProductCategory.BANNER),
    JOB_DETAIL_BANNER(3, "공고 상세", AdvertisementProductCategory.BANNER),
    BOOTCAMP_LIST_TOP_BANNER(4, "부트캠프 목록 상단", AdvertisementProductCategory.BANNER),
    BOOTCAMP_LIST_BOTTOM_BANNER(5, "부트캠프 목록 하단", AdvertisementProductCategory.BANNER),
    BOOTCAMP_DETAIL_BANNER(6, "부트캠프 상세", AdvertisementProductCategory.BANNER),
    JOB_LIST(7, "채용공고 목록", AdvertisementProductCategory.JOB_POSTING),
    BOOTCAMP_LIST(8, "부트캠프 목록 상단 고정", AdvertisementProductCategory.JOB_POSTING),
    JOB_DETAIL_SIDE(9, "공고 상세 옆 추천", AdvertisementProductCategory.JOB_POSTING),
    JOB_PICK(10, "오공고 PICK", AdvertisementProductCategory.JOB_POSTING),
    CALENDAR_DAY(11, "공고 달력 날짜별", AdvertisementProductCategory.JOB_POSTING),
    MYPAGE_SCRAP(12, "마이페이지 스크랩 칸", AdvertisementProductCategory.JOB_POSTING),
    ;
}
