package com.ogonggo.userapi.notification.channel.alimtalk

/**
 * NHN 요청에 넣을 수신 번호 형식만 허용한다.
 * 임의 문자를 제거하는 느슨한 정규화는 잘못된 주소를 유효한 번호처럼 바꿀 수 있어 허용 형식만 숫자로 변환한다.
 */
internal object AlimTalkRecipientNumberPolicy {

    private val PHONE_NUMBER_PATTERN = Regex("010(?:[0-9]{8}|-[0-9]{4}-[0-9]{4})")

    fun normalize(phoneNumber: String?): String? = phoneNumber
        ?.takeIf(PHONE_NUMBER_PATTERN::matches)
        ?.replace("-", "")
}
