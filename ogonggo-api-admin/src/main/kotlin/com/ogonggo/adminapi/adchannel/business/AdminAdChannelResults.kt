package com.ogonggo.adminapi.adchannel.business

import com.ogonggo.core.adchannel.domain.AdChannel
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import java.time.LocalDateTime

data class AdminAdChannelResult(
    val id: Long,
    val platform: AdChannelPlatform,
    val name: String,
    val handle: String?,
    val url: String,
    val profileImageUrl: String?,
    val memberCount: Int?,
    val enabled: Boolean,
    val memo: String?,
    val adCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun of(channel: AdChannel, adCount: Int): AdminAdChannelResult = AdminAdChannelResult(
            id = channel.requiredId(),
            platform = channel.platform,
            name = channel.name,
            handle = channel.handle,
            url = channel.url,
            profileImageUrl = channel.profileImageUrl,
            memberCount = channel.memberCount,
            enabled = channel.enabled,
            memo = channel.memo,
            adCount = adCount,
            createdAt = channel.createdAt,
            updatedAt = channel.updatedAt,
        )
    }
}

internal fun AdChannel.requiredId(): Long = checkNotNull(id) { "채널 식별자가 없습니다." }
