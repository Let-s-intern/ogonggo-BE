package com.ogonggo.core.advertisementproduct.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 광고 상품을 묶는 단위다. 광고 신청 폼에서 기업 담당자는 상품이 아니라 이 카테고리를 고른다.
 *
 * 인스타그램·카카오톡은 채널마다 하나다. 인스타그램은 오공고·렛츠커리어 계정, 카카오톡은 직무별 채용공고 큐레이션 톡방(6개)과
 * 렛츠커리어 공식 오픈채팅방(쥬디 톡방, 무료 세미나방 등)이다(2026-11-09 이름 정함, LC-3588). 코드 값은 그대로 둔다.
 */
enum class AdvertisementProductCategory(
    override val code: Int,
    override val desc: String,
) : EnumField {
    JOB_POSTING(1, "광고 공고"),
    BANNER(2, "배너"),
    INSTAGRAM_1(3, "인스타그램 오공고"),
    INSTAGRAM_2(4, "인스타그램 렛츠커리어"),
    KAKAO_1(5, "카카오톡 직무별 큐레이션 톡방"),
    KAKAO_2(6, "카카오톡 렛츠커리어 공식 오픈채팅방"),
    ;

    /** 오공고 화면에 직접 노출하는 카테고리다. 나머지는 운영자가 채널에 직접 올리므로 노출 설정이 없다. */
    val exposable: Boolean
        get() = this == JOB_POSTING || this == BANNER
}
