package com.ogonggo.adminapi.adchannelformat.business

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannelformat.domain.AdChannelFormat
import java.time.LocalDateTime

data class AdminAdChannelFormatResult(
    val id: Long,
    val platform: AdChannelPlatform,
    val name: String,
    val description: String?,
    val enabled: Boolean,
    val defaultPrice: Long?,
    val channelIds: List<Long>,
    val displayOrder: Int,
    val adCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun of(format: AdChannelFormat, adCount: Int): AdminAdChannelFormatResult = AdminAdChannelFormatResult(
            id = format.requiredId(),
            platform = format.platform,
            name = format.name,
            description = format.description,
            enabled = format.enabled,
            defaultPrice = format.defaultPrice,
            channelIds = format.channelIds,
            displayOrder = format.displayOrder,
            adCount = adCount,
            createdAt = format.createdAt,
            updatedAt = format.updatedAt,
        )
    }
}

internal fun AdChannelFormat.requiredId(): Long = checkNotNull(id) { "게재 유형 식별자가 없습니다." }
