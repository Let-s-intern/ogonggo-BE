package com.ogonggo.adminapi.channelad.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.implement.AdChannelReader
import com.ogonggo.core.adchannelformat.implement.AdChannelFormatReader
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.channelad.domain.ChannelAd
import com.ogonggo.core.channelad.domain.ChannelAdTerms
import com.ogonggo.core.channelad.implement.ChannelAdAppender
import com.ogonggo.core.channelad.implement.ChannelAdManager
import com.ogonggo.core.channelad.implement.ChannelAdReader
import com.ogonggo.core.channelad.implement.dto.ChannelAdAppendDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminChannelAdService(
    private val channelAdReader: ChannelAdReader,
    private val channelAdAppender: ChannelAdAppender,
    private val channelAdManager: ChannelAdManager,
    private val advertiserReader: AdvertiserReader,
    private val advertiserContractReader: AdvertiserContractReader,
    private val adChannelReader: AdChannelReader,
    private val adChannelFormatReader: AdChannelFormatReader,
    private val clock: Clock,
) {

    /** 게시일, 식별자 순이다. */
    fun getChannelAds(command: AdminChannelAdSearchCommand): List<AdminChannelAdResult> =
        toResults(channelAdReader.readAll(command.toCondition()))

    fun getChannelAd(adId: Long): AdminChannelAdResult = toResults(listOf(channelAdReader.read(adId))).single()

    /** 채널 광고를 등록하고 식별자를 돌려준다. */
    @Transactional
    fun createChannelAd(command: AdminChannelAdCreateCommand): Long =
        channelAdAppender.append(ChannelAdAppendDto(toTerms(command.toDraft(), previous = null))).requiredId()

    /** 보낸 칸만 바꾼다. 검증은 저장된 값과 합친 결과로 한다. */
    @Transactional
    fun updateChannelAd(adId: Long, command: AdminChannelAdUpdateCommand) {
        val ad = channelAdReader.readForUpdate(adId)
        channelAdManager.change(ad, toTerms(command.mergeInto(ad), previous = ad))
    }

    /** 이미 지운 광고를 다시 지워도 성공한다. */
    @Transactional
    fun deleteChannelAd(adId: Long) {
        channelAdManager.delete(channelAdReader.readForDelete(adId), LocalDateTime.now(clock))
    }

    /**
     * 프런트 목과 같은 순서(광고주 → 계약 → 채널 → 유형 → 날짜·상태·주소·메모)로 보고 처음 걸린 칸 하나만 알린다.
     * 사용 안 함인 채널·유형은 새로 고를 수 없고, 이미 그 값으로 잡힌 광고([previous])는 그대로 둘 수 있다.
     */
    private fun toTerms(draft: AdminChannelAdDraft, previous: ChannelAd?): ChannelAdTerms {
        val advertiserId = draft.advertiserId?.takeIf { advertiserReader.readAllByIds(listOf(it)).isNotEmpty() }
            ?: throw AdminConsoleBadRequestException.field("advertiserId", "있는 광고주를 골라 주세요.")
        draft.contractId?.let { contractId ->
            val contract = advertiserContractReader.readAllByIds(listOf(contractId))[contractId]
                ?: throw AdminConsoleBadRequestException.field("contractId", "있는 계약을 골라 주세요.")
            if (contract.advertiserId != advertiserId) {
                throw AdminConsoleBadRequestException.field("contractId", "고른 광고주의 계약만 이을 수 있습니다.")
            }
        }
        val channel = draft.channelId?.let(adChannelReader::readOrNull)
            ?: throw AdminConsoleBadRequestException.field("channelId", "있는 채널을 골라 주세요.")
        if (!channel.enabled && previous?.channelId != channel.id) {
            throw AdminConsoleBadRequestException.field("channelId", "사용 안 함인 채널은 고를 수 없습니다.")
        }
        val format = draft.formatId?.let(adChannelFormatReader::readOrNull)
            ?: throw AdminConsoleBadRequestException.field("formatId", "있는 게재 유형을 골라 주세요.")
        if (!format.enabled && previous?.formatId != format.id) {
            throw AdminConsoleBadRequestException.field("formatId", "사용 안 함인 게재 유형은 고를 수 없습니다.")
        }
        if (format.platform != channel.platform) {
            throw AdminConsoleBadRequestException.field("formatId", "채널과 같은 플랫폼의 게재 유형을 골라 주세요.")
        }
        val channelId = checkNotNull(channel.id)
        if (!format.allows(channelId)) {
            throw AdminConsoleBadRequestException.field("formatId", "${channel.label} 에는 ${format.name} 을(를) 올리지 않습니다.")
        }
        return draft.toTerms(advertiserId = advertiserId, platform = channel.platform, channelId = channelId, formatId = checkNotNull(format.id))
    }

    private fun toResults(ads: List<ChannelAd>): List<AdminChannelAdResult> {
        val advertisers = advertiserReader.readAllByIds(ads.map { it.advertiserId }.toSet())
        val contracts = advertiserContractReader.readAllByIds(ads.mapNotNull { it.contractId }.toSet())
        val channels = adChannelReader.readAllByIds(ads.map { it.channelId }.toSet())
        val formats = adChannelFormatReader.readAllByIds(ads.map { it.formatId }.toSet())
        return ads.map {
            AdminChannelAdResult.of(
                ad = it,
                advertiser = advertisers[it.advertiserId],
                contract = it.contractId?.let(contracts::get),
                channel = channels[it.channelId],
                format = formats[it.formatId],
            )
        }
    }
}
