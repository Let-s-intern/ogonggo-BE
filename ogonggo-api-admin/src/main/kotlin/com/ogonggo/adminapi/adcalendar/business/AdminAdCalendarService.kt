package com.ogonggo.adminapi.adcalendar.business

import com.ogonggo.adminapi.adposting.business.AdminAdPostingResult
import com.ogonggo.adminapi.banner.business.AdminBannerResult
import com.ogonggo.adminapi.calendartodo.business.AdminCalendarDateRange
import com.ogonggo.adminapi.channelad.business.AdminChannelAdResult
import com.ogonggo.core.adchannel.implement.AdChannelReader
import com.ogonggo.core.adchannelformat.implement.AdChannelFormatReader
import com.ogonggo.core.adposting.implement.AdPostingReader
import com.ogonggo.core.adposting.implement.AdPostingTargetReader
import com.ogonggo.core.adposting.implement.dto.AdPostingTargetDto
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.banner.implement.BannerReader
import com.ogonggo.core.channelad.domain.ChannelAdSearchCondition
import com.ogonggo.core.channelad.implement.ChannelAdReader
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

/**
 * 관리자 콘솔 캘린더의 광고 일정이다. 새로 저장하는 것 없이 계약·배너·광고 공고·채널 광고를 기간으로 한 번씩 읽고,
 * 광고주·계약·대상·채널·게재 유형은 종류마다 한 번에 읽어 붙인다.
 */
@Service
class AdminAdCalendarService(
    private val advertiserContractReader: AdvertiserContractReader,
    private val bannerReader: BannerReader,
    private val adPostingReader: AdPostingReader,
    private val adPostingTargetReader: AdPostingTargetReader,
    private val channelAdReader: ChannelAdReader,
    private val adChannelReader: AdChannelReader,
    private val adChannelFormatReader: AdChannelFormatReader,
    private val advertiserReader: AdvertiserReader,
    private val clock: Clock,
) {

    /**
     * [range]와 겹치는 광고 일정 전부다. 꺼진 배너·광고 공고와 취소한 채널 광고도 싣는다.
     * 계약·배너·광고 공고는 시각 기간(시작 포함·끝 미포함)을 `[from 00:00, to 00:00)`과 견주고, 배너·광고 공고의 null 기간은
     * 그쪽이 열린 것으로 본다. 채널 광고는 날짜(게시일 ~ 끝 날, 둘 다 포함)로 견준다.
     */
    fun getAdCalendar(range: AdminCalendarDateRange): AdminAdCalendarResult {
        val from = range.from.atStartOfDay()
        val toExclusive = range.toExclusive.atStartOfDay()
        val contracts = advertiserContractReader.readAllOverlapping(from, toExclusive)
        val banners = bannerReader.readAllOverlapping(from, toExclusive)
        val adPostings = adPostingReader.readAllOverlapping(from, toExclusive)
        val channelAds = channelAdReader.readAll(ChannelAdSearchCondition(from = range.from, toExclusive = range.toExclusive))

        val advertisers = advertiserReader.readAllByIds(
            contracts.map { it.advertiserId } + banners.mapNotNull { it.advertiserId } +
                adPostings.map { it.advertiserId } + channelAds.map { it.advertiserId },
        )
        val linkedContracts = advertiserContractReader.readAllByIds(
            banners.mapNotNull { it.contractId } + adPostings.mapNotNull { it.contractId } + channelAds.mapNotNull { it.contractId },
        )
        val targets = if (adPostings.isEmpty()) emptyMap() else adPostingTargetReader.read(adPostings, LocalDateTime.now(clock))
        val channels = adChannelReader.readAllByIds(channelAds.map { it.channelId }.toSet())
        val formats = adChannelFormatReader.readAllByIds(channelAds.map { it.formatId }.toSet())

        return AdminAdCalendarResult(
            contracts = contracts.map { AdminAdCalendarContractResult.of(it, advertisers[it.advertiserId]) },
            banners = banners.map {
                AdminBannerResult.of(it, it.advertiserId?.let(advertisers::get), it.contractId?.let(linkedContracts::get))
            },
            adPostings = adPostings.map {
                AdminAdPostingResult.of(
                    adPosting = it,
                    target = targets[checkNotNull(it.id)] ?: AdPostingTargetDto.MISSING,
                    advertiser = advertisers[it.advertiserId],
                    contract = it.contractId?.let(linkedContracts::get),
                )
            },
            channelAds = channelAds.map {
                AdminChannelAdResult.of(
                    ad = it,
                    advertiser = advertisers[it.advertiserId],
                    contract = it.contractId?.let(linkedContracts::get),
                    channel = channels[it.channelId],
                    format = formats[it.formatId],
                )
            },
        )
    }
}
