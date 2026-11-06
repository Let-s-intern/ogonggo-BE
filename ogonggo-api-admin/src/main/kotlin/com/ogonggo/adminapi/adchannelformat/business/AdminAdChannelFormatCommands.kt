package com.ogonggo.adminapi.adchannelformat.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannelformat.domain.AdChannelFormat
import com.ogonggo.core.adchannelformat.domain.AdChannelFormatTerms
import java.util.Optional

/** 게재 유형 등록 본문이다. 검증 전 값을 그대로 담는다. 사용 여부를 보내지 않으면 켠 유형, 채널을 보내지 않으면 모든 채널이다. */
data class AdminAdChannelFormatCreateCommand(
    val platform: AdChannelPlatform?,
    val name: String?,
    val description: String?,
    val enabled: Boolean?,
    val defaultPrice: Long?,
    val channelIds: List<Long?>?,
) {
    internal fun toDraft(): AdminAdChannelFormatDraft = AdminAdChannelFormatDraft(
        platform = platform,
        name = name,
        description = description,
        enabled = enabled ?: true,
        defaultPrice = defaultPrice,
        channelIds = channelIds.orEmpty(),
    )
}

/**
 * 게재 유형 수정 본문이다. null이면 저장된 값을 그대로 둔다. null을 보낼 수 있는 칸(`description`, `defaultPrice`)은
 * 보내지 않으면 null, null을 보내면 빈 [Optional]이다. `platform`은 저장된 값과 다르면 거절한다.
 */
data class AdminAdChannelFormatUpdateCommand(
    val platform: AdChannelPlatform? = null,
    val name: String? = null,
    val description: Optional<String>? = null,
    val enabled: Boolean? = null,
    val defaultPrice: Optional<Long>? = null,
    val channelIds: List<Long?>? = null,
) {
    /** 저장된 값 위에 보낸 칸을 얹는다. 검증은 합친 결과로 한다. */
    internal fun mergeInto(format: AdChannelFormat): AdminAdChannelFormatDraft {
        if (platform != null && platform != format.platform) {
            throw AdminConsoleBadRequestException.field("platform", "등록한 뒤에는 바꿀 수 없습니다.")
        }
        return AdminAdChannelFormatDraft(
            platform = format.platform,
            name = name ?: format.name,
            description = if (description == null) format.description else description.orElse(null),
            enabled = enabled ?: format.enabled,
            defaultPrice = if (defaultPrice == null) format.defaultPrice else defaultPrice.orElse(null),
            channelIds = channelIds ?: format.channelIds,
        )
    }
}

/** 한 플랫폼의 게재 유형 순서다. 그 플랫폼의 유형 id를 빠짐없이, 겹치지 않게 담아야 한다. */
data class AdminAdChannelFormatOrderCommand(
    val platform: AdChannelPlatform?,
    val formatIds: List<Long?>?,
) {
    /** [currentFormatIds]는 그 플랫폼의 유형 전부다. */
    internal fun validFormatIds(currentFormatIds: Collection<Long>): List<Long> {
        val formatIds = formatIds
        if (formatIds == null || formatIds.any { it == null } || formatIds.size != currentFormatIds.size ||
            formatIds.toSet() != currentFormatIds.toSet()
        ) {
            throw AdminConsoleBadRequestException.field("formatIds", "그 플랫폼의 게재 유형 id 를 빠짐없이, 겹치지 않게 보내 주세요.")
        }
        return formatIds.filterNotNull()
    }
}

internal fun platformError(): AdminConsoleBadRequestException = AdminConsoleBadRequestException.field(
    "platform",
    "${AdChannelPlatform.entries.joinToString(", ") { it.name }} 가운데 하나로 보내 주세요.",
)

/** 등록 본문이나 저장된 값과 합친 수정 본문이다. 프런트 목과 같은 순서로 보고 처음 걸린 칸 하나만 알린다. */
internal data class AdminAdChannelFormatDraft(
    val platform: AdChannelPlatform?,
    val name: String?,
    val description: String?,
    val enabled: Boolean?,
    val defaultPrice: Long?,
    val channelIds: List<Long?>,
) {
    /**
     * [nameTaken]은 다듬은 이름이 같은 플랫폼의 다른 유형에 있는지 보고, [channelPlatforms]는 있는 채널 식별자마다 그 플랫폼을 준다.
     */
    fun toTerms(
        nameTaken: (AdChannelPlatform, String) -> Boolean,
        channelPlatforms: Map<Long, AdChannelPlatform>,
    ): Pair<AdChannelPlatform, AdChannelFormatTerms> {
        val platform = platform ?: throw platformError()
        val name = name?.trim().orEmpty()
        if (name.isEmpty() || name.length > AdChannelFormatTerms.MAX_NAME_LENGTH) {
            throw AdminConsoleBadRequestException.field("name", "1~${AdChannelFormatTerms.MAX_NAME_LENGTH}자로 보내 주세요.")
        }
        if (nameTaken(platform, name)) {
            throw AdminConsoleBadRequestException.field("name", "같은 플랫폼에 같은 이름의 게재 유형이 있습니다.")
        }
        if ((description?.trim()?.length ?: 0) > AdChannelFormatTerms.MAX_DESCRIPTION_LENGTH) {
            throw AdminConsoleBadRequestException.field(
                "description",
                "${AdChannelFormatTerms.MAX_DESCRIPTION_LENGTH}자까지 보낼 수 있습니다.",
            )
        }
        val enabled = enabled ?: throw AdminConsoleBadRequestException.field("enabled", "참·거짓 값을 보내 주세요.")
        if (defaultPrice != null && defaultPrice !in 0..AdChannelFormatTerms.MAX_PRICE) {
            throw AdminConsoleBadRequestException.field("defaultPrice", "null 이거나 0 이상의 정수(원)로 보내 주세요.")
        }
        if (channelIds.any { it == null || channelPlatforms[it] != platform }) {
            throw AdminConsoleBadRequestException.field("channelIds", "같은 플랫폼에 있는 채널 id 만 보내 주세요.")
        }
        return platform to AdChannelFormatTerms(
            name = name,
            description = description,
            enabled = enabled,
            defaultPrice = defaultPrice,
            channelIds = channelIds.filterNotNull(),
        )
    }
}
