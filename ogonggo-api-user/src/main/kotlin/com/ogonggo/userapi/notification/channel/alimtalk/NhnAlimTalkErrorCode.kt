package com.ogonggo.userapi.notification.channel.alimtalk

import com.ogonggo.core.notification.domain.NotificationFailureCategory

/** NHN의 API 요청 응답 코드만 분류한다. 실제 카카오 전달 결과 코드는 조회하지 않는다. */
internal object NhnAlimTalkErrorCode {
    private val knownCodes = mapOf(
        -1000 to Entry(
            NotificationFailureCategory.AUTHENTICATION_CONFIGURATION,
            "유효하지 않은 NHN 앱키",
        ),
        -1001 to Entry(
            NotificationFailureCategory.AUTHENTICATION_CONFIGURATION,
            "유효하지 않은 NHN 비밀 키",
        ),
        -1005 to Entry(
            NotificationFailureCategory.DUPLICATE_REQUEST,
            "동일한 멱등성 키로 10분 이내 요청됨. 이전 요청의 최종 전달 여부는 별도 확인 필요",
        ),
        -1006 to Entry(
            NotificationFailureCategory.SENDER_PROFILE_CONFIGURATION,
            "발신 프로필에 발신 키가 없음",
        ),
        -1027 to Entry(
            NotificationFailureCategory.SENDER_PROFILE_CONFIGURATION,
            "발신 프로필이 차단 상태",
        ),
        -2017 to Entry(
            NotificationFailureCategory.SENDER_PROFILE_CONFIGURATION,
            "발신 프로필을 찾을 수 없음",
        ),
        -3003 to Entry(
            NotificationFailureCategory.TEMPLATE_CONFIGURATION,
            "템플릿을 찾을 수 없음",
        ),
        -3004 to Entry(
            NotificationFailureCategory.TEMPLATE_CONFIGURATION,
            "템플릿 파라미터 오류",
        ),
        -3005 to Entry(
            NotificationFailureCategory.TEMPLATE_CONFIGURATION,
            "템플릿 승인 상태 오류",
        ),
    )

    fun resolve(resultCode: Int?): Entry = when {
        resultCode == null -> Entry(
            NotificationFailureCategory.UNKNOWN_PROVIDER_ERROR,
            "NHN 응답에 결과 코드가 없음",
        )
        resultCode in knownCodes -> knownCodes.getValue(resultCode)
        resultCode == 4 || resultCode in -2005..-2000 || resultCode == -2018 || resultCode == -2504 -> Entry(
            NotificationFailureCategory.INVALID_REQUEST,
            "요청 파라미터 검증 오류",
        )
        resultCode in setOf(
            -2033,
            -2034,
            -2036,
            -2037,
            -3000,
            -3002,
            -3006,
            -3007,
            -3008,
            -3009,
            -3010,
        ) -> Entry(
            NotificationFailureCategory.TEMPLATE_CONFIGURATION,
            "템플릿·버튼·메시지 내용 검증 오류",
        )
        else -> Entry(
            NotificationFailureCategory.UNKNOWN_PROVIDER_ERROR,
            "매핑되지 않은 NHN API 오류 코드",
        )
    }

    data class Entry(
        val category: NotificationFailureCategory,
        val description: String,
    )
}
