package com.ogonggo.userapi.notification.intake.implement.signup.dto

import com.ogonggo.userapi.notification.channel.alimtalk.dto.SignUpAlimTalkParametersDto

/** 가입 이벤트를 알림 적재에 필요한 완성된 템플릿 값 또는 생략 사유로 변환한 결과다. */
internal sealed interface SignUpAlimTalkPreparationDto {
    data class Ready(
        val templateCode: String,
        val recipientNo: String,
        val templateParameters: SignUpAlimTalkParametersDto,
    ) : SignUpAlimTalkPreparationDto

    data class Skipped(val reason: SignUpAlimTalkSkipReason) : SignUpAlimTalkPreparationDto
}

/** 개인정보 원문 없이 가입 알림 생략 이유를 식별한다. */
internal enum class SignUpAlimTalkSkipReason {
    MISSING_NAME,
    MISSING_EMAIL,
    MISSING_PHONE_NUMBER,
    INVALID_PHONE_NUMBER,
    MISSING_AUTH_PROVIDER,
}
