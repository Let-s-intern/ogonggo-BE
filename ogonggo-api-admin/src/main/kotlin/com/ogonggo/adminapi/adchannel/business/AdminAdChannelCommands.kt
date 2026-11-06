package com.ogonggo.adminapi.adchannel.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.domain.AdChannel
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannel.domain.AdChannelProfile
import java.util.Optional

/** 채널 등록 본문이다. 검증 전 값을 그대로 담는다. 사용 여부를 보내지 않으면 켠 채널이다. */
data class AdminAdChannelCreateCommand(
    val platform: AdChannelPlatform?,
    val name: String?,
    val handle: String?,
    val url: String?,
    val profileImageUrl: String?,
    val memberCount: Long?,
    val enabled: Boolean?,
    val memo: String?,
) {
    internal fun toDraft(): AdminAdChannelDraft = AdminAdChannelDraft(
        platform = platform,
        name = name,
        handle = handle,
        url = url,
        profileImageUrl = profileImageUrl,
        memberCount = memberCount,
        enabled = enabled ?: true,
        memo = memo,
    )
}

/**
 * 채널 수정 본문이다. null이면 저장된 값을 그대로 둔다. null을 보낼 수 있는 칸(`profileImageUrl`, `memberCount`, `memo`)은
 * 보내지 않으면 null, null을 보내면 빈 [Optional]이다. `platform`은 저장된 값과 다르면 거절한다.
 */
data class AdminAdChannelUpdateCommand(
    val platform: AdChannelPlatform? = null,
    val name: String? = null,
    val handle: String? = null,
    val url: String? = null,
    val profileImageUrl: Optional<String>? = null,
    val memberCount: Optional<Long>? = null,
    val enabled: Boolean? = null,
    val memo: Optional<String>? = null,
) {
    /** 저장된 값 위에 보낸 칸을 얹는다. 검증은 합친 결과로 한다. */
    internal fun mergeInto(channel: AdChannel): AdminAdChannelDraft {
        if (platform != null && platform != channel.platform) {
            throw AdminConsoleBadRequestException.field("platform", "등록한 뒤에는 바꿀 수 없습니다.")
        }
        return AdminAdChannelDraft(
            platform = channel.platform,
            name = name ?: channel.name,
            handle = handle ?: channel.handle,
            url = url ?: channel.url,
            profileImageUrl = if (profileImageUrl == null) channel.profileImageUrl else profileImageUrl.orElse(null),
            memberCount = if (memberCount == null) channel.memberCount?.toLong() else memberCount.orElse(null),
            enabled = enabled ?: channel.enabled,
            memo = if (memo == null) channel.memo else memo.orElse(null),
        )
    }
}

/** 등록 본문이나 저장된 값과 합친 수정 본문이다. 프런트 목과 같은 순서로 보고 처음 걸린 칸 하나만 알린다. */
internal data class AdminAdChannelDraft(
    val platform: AdChannelPlatform?,
    val name: String?,
    val handle: String?,
    val url: String?,
    val profileImageUrl: String?,
    val memberCount: Long?,
    val enabled: Boolean?,
    val memo: String?,
) {
    /** 오픈채팅은 계정 이름을 보내도 버린다. [handleTaken]은 다듬은 계정 이름이 다른 채널에 있는지 본다. */
    fun toProfile(handleTaken: (String) -> Boolean): Pair<AdChannelPlatform, AdChannelProfile> {
        val platform = platform ?: throw AdminConsoleBadRequestException.field(
            "platform",
            "${AdChannelPlatform.entries.joinToString(", ") { it.name }} 가운데 하나로 보내 주세요.",
        )
        val handle = if (platform == AdChannelPlatform.INSTAGRAM) {
            val normalized = AdChannelProfile.normalizeHandle(handle.orEmpty())
            if (!AdChannelProfile.HANDLE.matches(normalized)) {
                throw AdminConsoleBadRequestException.field("handle", "영문·숫자·마침표·밑줄 30자까지로 보내 주세요(`@` 는 빼도 됩니다).")
            }
            if (handleTaken(normalized)) {
                throw AdminConsoleBadRequestException.field("handle", "이미 등록된 계정입니다.")
            }
            normalized
        } else {
            null
        }
        val name = name?.trim().orEmpty()
        if (name.isEmpty() || name.length > AdChannelProfile.MAX_NAME_LENGTH) {
            throw AdminConsoleBadRequestException.field("name", "1~${AdChannelProfile.MAX_NAME_LENGTH}자로 보내 주세요.")
        }
        val url = url?.trim().orEmpty()
        if (!AdChannelProfile.URL.matches(url)) {
            throw AdminConsoleBadRequestException.field("url", "http:// 또는 https:// 로 시작하는 주소를 보내 주세요.")
        }
        if (url.length > AdChannelProfile.MAX_URL_LENGTH) {
            throw AdminConsoleBadRequestException.field("url", "${AdChannelProfile.MAX_URL_LENGTH}자까지 보낼 수 있습니다.")
        }
        val profileImageUrl = profileImageUrl?.trim()?.ifEmpty { null }
        if (profileImageUrl != null && !AdChannelProfile.IMAGE_URL.matches(profileImageUrl)) {
            throw AdminConsoleBadRequestException.field("profileImageUrl", "null 이거나 https:// 주소, / 로 시작하는 경로로 보내 주세요.")
        }
        if ((profileImageUrl?.length ?: 0) > AdChannelProfile.MAX_URL_LENGTH) {
            throw AdminConsoleBadRequestException.field("profileImageUrl", "${AdChannelProfile.MAX_URL_LENGTH}자까지 보낼 수 있습니다.")
        }
        if (memberCount != null && memberCount !in 0..AdChannelProfile.MAX_MEMBER_COUNT.toLong()) {
            throw AdminConsoleBadRequestException.field("memberCount", "null 이거나 0 이상의 정수로 보내 주세요.")
        }
        val enabled = enabled ?: throw AdminConsoleBadRequestException.field("enabled", "참·거짓 값을 보내 주세요.")
        if ((memo?.trim()?.length ?: 0) > AdChannelProfile.MAX_MEMO_LENGTH) {
            throw AdminConsoleBadRequestException.field("memo", "${AdChannelProfile.MAX_MEMO_LENGTH}자까지 보낼 수 있습니다.")
        }
        return platform to AdChannelProfile(
            name = name,
            handle = handle,
            url = url,
            profileImageUrl = profileImageUrl,
            memberCount = memberCount?.toInt(),
            enabled = enabled,
            memo = memo,
        )
    }
}
