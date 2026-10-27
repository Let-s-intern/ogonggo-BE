package com.ogonggo.core.advertisementinquiry.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 문의가 어디서 들어왔는지를 저장할 때 한 번 판정한 값이다. 어드민 필터와 슬랙 알림이 같은 값을 쓴다.
 *
 * 판정이 틀렸을 때 확인할 수 있도록 판정에 쓴 원본(UTM, referrer)도 문의에 함께 남긴다.
 * [UTM_OTHER]는 정해 두지 않은 `utm_source`이고, 화면에는 그 원문을 보인다.
 * [EXTERNAL_SITE]는 화면에 referrer 호스트를 함께 보인다.
 */
enum class AdvertisementInquiryInflowChannel(
    override val code: Int,
    override val desc: String,
) : EnumField {
    COLDMAIL(1, "콜드메일"),
    INSTAGRAM(2, "인스타그램"),
    KAKAO(3, "카카오톡"),
    NEWSLETTER(4, "뉴스레터"),
    UTM_OTHER(5, "기타 UTM"),
    OGONGGO(6, "오공고 사이트"),
    LETSCAREER(7, "렛츠커리어"),
    SEARCH(8, "검색"),
    EXTERNAL_SITE(9, "외부 사이트"),
    DIRECT(10, "직접 접속"),
}
