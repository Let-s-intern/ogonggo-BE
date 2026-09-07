package com.ogonggo.userapi.advertisement.business

import com.ogonggo.core.enumeration.EnumField

/**
 * 기업 담당자가 무료 홍보를 원하는 렛츠커리어 채널이다. 한 곳만 고른다.
 *
 * 폼에 보이는 팔로워 수와 인원 수는 계속 바뀌므로 여기 담지 않는다.
 * 서버는 어느 채널인지만 알면 되고, 규모 표기는 폼이 소유한다.
 */
enum class AdvertisementPromotionChannel(
    override val code: Int,
    override val desc: String,
) : EnumField {
    INSTAGRAM(1, "인스타그램 @letscareer.job"),
    OPEN_CHAT_MARKETING(2, "오픈채팅방 · 마케팅"),
    OPEN_CHAT_PLANNING(3, "오픈채팅방 · 기획·운영"),
    OPEN_CHAT_HR(4, "오픈채팅방 · 인사·HR·경영관리"),
    OPEN_CHAT_PUBLIC_RECRUITMENT(5, "오픈채팅방 · 공채 전반"),
    OPEN_CHAT_SALES(6, "오픈채팅방 · 세일즈"),
    OPEN_CHAT_AI_DEVELOPMENT(7, "오픈채팅방 · AI역량·개발"),

    /** 담당자가 고르지 못한 경우다. 영업이 채널을 제안해야 하는 건으로 구분한다. */
    RECOMMENDATION_REQUESTED(8, "정하기 어렵습니다. 추천해주세요"),
}
