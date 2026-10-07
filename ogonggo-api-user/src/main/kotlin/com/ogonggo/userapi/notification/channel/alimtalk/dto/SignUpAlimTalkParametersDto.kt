package com.ogonggo.userapi.notification.channel.alimtalk.dto

/** NHN에 등록된 가입 완료 템플릿의 변수 이름과 payload 구조를 고정한다. */
internal data class SignUpAlimTalkParametersDto(
    val name: String,
    val userEmail: String,
    val loginType: String,
    val createDate: String,
)
