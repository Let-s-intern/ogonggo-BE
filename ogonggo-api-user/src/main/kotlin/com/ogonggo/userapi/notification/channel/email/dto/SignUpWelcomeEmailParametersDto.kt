package com.ogonggo.userapi.notification.channel.email.dto

import com.fasterxml.jackson.annotation.JsonInclude

/** 가입 환영 메일 템플릿의 변수 이름과 payload 구조를 고정한다. 이름이 없으면 payload에서 뺀다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class SignUpWelcomeEmailParametersDto(
    val name: String?,
)
