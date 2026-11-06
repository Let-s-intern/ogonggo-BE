package com.ogonggo.core.adchannel.domain

import com.ogonggo.core.enumeration.EnumField

/** 채널 광고를 올리는 우리 채널의 플랫폼이다. 채널·게재 유형은 등록한 뒤 플랫폼을 바꾸지 않는다. 선언 순서가 화면의 탭·목록 순서다. */
enum class AdChannelPlatform(
    override val code: Int,
    override val desc: String,
) : EnumField {
    INSTAGRAM(1, "인스타그램"),
    KAKAO_OPENCHAT(2, "카카오톡 오픈채팅"),
}
