package com.ogonggo.core.channelad.domain

import com.ogonggo.core.enumeration.EnumField

/** 채널 광고 진행 상태다. 운영자가 바꾸며 날짜로 저절로 바뀌지 않는다. */
enum class ChannelAdStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    SCHEDULED(1, "예정"),
    PUBLISHED(2, "게시 완료"),
    CANCELED(3, "취소"),
}
