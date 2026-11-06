package com.ogonggo.adminapi.channelad.business

import com.ogonggo.core.adchannel.domain.AdChannel
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannelformat.domain.AdChannelFormat
import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.channelad.domain.ChannelAd
import com.ogonggo.core.channelad.domain.ChannelAdStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminChannelAdResult(
    val id: Long,
    val advertiserId: Long,
    val advertiser: AdvertiserSummary?,
    val contractId: Long?,
    val contract: ContractSummary?,
    val platform: AdChannelPlatform,
    val channelId: Long,
    val channel: ChannelSummary?,
    val formatId: Long,
    val format: FormatSummary?,
    val postDate: LocalDate,
    val endDate: LocalDate?,
    val status: ChannelAdStatus,
    val postUrl: String?,
    val creativeMemo: String?,
    val adminMemo: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    data class AdvertiserSummary(val id: Long, val name: String, val type: AdvertiserType)

    data class ContractSummary(val id: Long, val name: String?, val startAt: LocalDateTime, val endAt: LocalDateTime)

    data class ChannelSummary(val id: Long, val platform: AdChannelPlatform, val name: String, val handle: String?)

    data class FormatSummary(val id: Long, val platform: AdChannelPlatform, val name: String)

    companion object {
        internal fun of(
            ad: ChannelAd,
            advertiser: Advertiser?,
            contract: AdvertiserContract?,
            channel: AdChannel?,
            format: AdChannelFormat?,
        ): AdminChannelAdResult = AdminChannelAdResult(
            id = ad.requiredId(),
            advertiserId = ad.advertiserId,
            advertiser = advertiser?.let { AdvertiserSummary(checkNotNull(it.id), it.name, it.type) },
            contractId = ad.contractId,
            contract = contract?.let { ContractSummary(checkNotNull(it.id), it.name, it.startAt, it.endAt) },
            platform = ad.platform,
            channelId = ad.channelId,
            channel = channel?.let { ChannelSummary(checkNotNull(it.id), it.platform, it.name, it.handle) },
            formatId = ad.formatId,
            format = format?.let { FormatSummary(checkNotNull(it.id), it.platform, it.name) },
            postDate = ad.postDate,
            endDate = ad.endDate,
            status = ad.status,
            postUrl = ad.postUrl,
            creativeMemo = ad.creativeMemo,
            adminMemo = ad.adminMemo,
            createdAt = ad.createdAt,
            updatedAt = ad.updatedAt,
        )
    }
}

internal fun ChannelAd.requiredId(): Long = checkNotNull(id) { "채널 광고 식별자가 없습니다." }
