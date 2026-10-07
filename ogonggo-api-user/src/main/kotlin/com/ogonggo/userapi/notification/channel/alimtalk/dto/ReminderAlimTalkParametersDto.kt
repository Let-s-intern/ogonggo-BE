package com.ogonggo.userapi.notification.channel.alimtalk.dto

import com.fasterxml.jackson.annotation.JsonProperty

/** NHN에 등록된 스크랩 리마인드 템플릿의 변수 이름과 payload 구조를 고정한다. */
internal data class ReminderAlimTalkParametersDto(
    val name: String,
    @field:JsonProperty("posting-title")
    val postingTitle: String,
)
