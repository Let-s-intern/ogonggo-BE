package com.ogonggo.adminapi.advertiser.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserContact

/** 비회원 광고주 추가 본문이다. 검증 전 값을 그대로 담는다. */
data class AdminAdvertiserCreateCommand(
    val name: String?,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
)

/** 비회원 광고주 수정 본문이다. null이면 그대로 두고 빈 문자열(공백만 포함)이면 그 칸을 지운다. */
data class AdminAdvertiserUpdateCommand(
    val name: String? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
)

/** 운영자가 적는 비회원 광고주 이름의 길이다. 회원 기관명은 회원 정보를 따르므로 이 제한을 받지 않는다. */
internal const val MAX_ADVERTISER_NAME_LENGTH = 50

private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** 앞뒤 공백을 뺀 이름이다. 1~50자가 아니면 화면 문구로 알린다. */
internal fun validAdvertiserName(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    if (trimmed.isEmpty() || trimmed.length > MAX_ADVERTISER_NAME_LENGTH) {
        throw AdminConsoleBadRequestException.field("name", "1~${MAX_ADVERTISER_NAME_LENGTH}자로 보내 주세요.")
    }
    return trimmed
}

/** 앞뒤 공백을 뺀 담당자 칸이다. 비어 있으면 빈 문자열을 돌려준다. 메일은 형식만 본다. */
internal fun validContactName(value: String): String = validLength("contactName", value.trim(), Advertiser.MAX_CONTACT_NAME_LENGTH)

internal fun validContactPhone(value: String): String = validLength("contactPhone", value.trim(), Advertiser.MAX_CONTACT_PHONE_LENGTH)

internal fun validContactEmail(value: String): String {
    val trimmed = validLength("contactEmail", value.trim(), Advertiser.MAX_CONTACT_EMAIL_LENGTH)
    if (trimmed.isNotEmpty() && !EMAIL.matches(trimmed)) {
        throw AdminConsoleBadRequestException.field("contactEmail", "메일 주소 형식으로 보내 주세요.")
    }
    return trimmed
}

internal fun AdminAdvertiserCreateCommand.validContact(): AdvertiserContact = AdvertiserContact(
    name = contactName?.let(::validContactName)?.ifEmpty { null },
    email = contactEmail?.let(::validContactEmail)?.ifEmpty { null },
    phone = contactPhone?.let(::validContactPhone)?.ifEmpty { null },
)

private fun validLength(fieldName: String, value: String, max: Int): String {
    if (value.length > max) {
        throw AdminConsoleBadRequestException.field(fieldName, "${max}자까지 보낼 수 있습니다.")
    }
    return value
}
