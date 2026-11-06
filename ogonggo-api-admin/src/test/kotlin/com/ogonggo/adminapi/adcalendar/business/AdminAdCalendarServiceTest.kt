package com.ogonggo.adminapi.adcalendar.business

import com.ogonggo.adminapi.calendartodo.business.AdminCalendarDateRange
import com.ogonggo.core.adchannel.implement.AdChannelReader
import com.ogonggo.core.adchannelformat.implement.AdChannelFormatReader
import com.ogonggo.core.adposting.implement.AdPostingReader
import com.ogonggo.core.adposting.implement.AdPostingTargetReader
import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisercontract.domain.AdContractProduct
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.banner.implement.BannerReader
import com.ogonggo.core.channelad.domain.ChannelAdSearchCondition
import com.ogonggo.core.channelad.implement.ChannelAdReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class AdminAdCalendarServiceTest {

    private val advertiserContractReader = Mockito.mock(AdvertiserContractReader::class.java)
    private val bannerReader = Mockito.mock(BannerReader::class.java)
    private val adPostingReader = Mockito.mock(AdPostingReader::class.java)
    private val channelAdReader = Mockito.mock(ChannelAdReader::class.java)
    private val advertiserReader = Mockito.mock(AdvertiserReader::class.java)
    private val service = AdminAdCalendarService(
        advertiserContractReader = advertiserContractReader,
        bannerReader = bannerReader,
        adPostingReader = adPostingReader,
        adPostingTargetReader = Mockito.mock(AdPostingTargetReader::class.java),
        channelAdReader = channelAdReader,
        adChannelReader = Mockito.mock(AdChannelReader::class.java),
        adChannelFormatReader = Mockito.mock(AdChannelFormatReader::class.java),
        advertiserReader = advertiserReader,
        clock = Clock.systemDefaultZone(),
    )

    @Test
    fun `계약은 조회 날짜의 0시로 견주고 광고주 요약을 붙이며 채널 광고는 날짜 기간으로 찾는다`() {
        // given
        val contract = contract()
        val advertiser = Mockito.mock(Advertiser::class.java)
        Mockito.`when`(advertiser.id).thenReturn(ADVERTISER_ID)
        Mockito.`when`(advertiser.name).thenReturn("그로우랩")
        Mockito.`when`(advertiser.type).thenReturn(AdvertiserType.NON_MEMBER)
        Mockito.`when`(advertiserContractReader.readAllOverlapping(FROM.atStartOfDay(), TO.atStartOfDay())).thenReturn(listOf(contract))
        Mockito.`when`(advertiserReader.readAllByIds(listOf(ADVERTISER_ID))).thenReturn(mapOf(ADVERTISER_ID to advertiser))

        // when
        val result = service.getAdCalendar(AdminCalendarDateRange(FROM, TO))

        // then
        assertEquals(
            AdminAdCalendarContractResult(
                id = 1,
                name = "11월 공고 상세 패키지",
                startAt = START,
                endAt = START.plusMonths(1),
                advertiserId = ADVERTISER_ID,
                advertiser = AdminAdCalendarAdvertiserResult(ADVERTISER_ID, "그로우랩", AdvertiserType.NON_MEMBER),
                amount = 1_500_000,
                paid = true,
                products = listOf(AdContractProduct.BANNER_JOB_DETAIL_SIDE),
            ),
            result.contracts.single(),
        )
        Mockito.verify(channelAdReader).readAll(ChannelAdSearchCondition(from = FROM, toExclusive = TO))
    }

    private fun contract(): AdvertiserContract = Mockito.mock(AdvertiserContract::class.java).also {
        Mockito.`when`(it.id).thenReturn(1L)
        Mockito.`when`(it.name).thenReturn("11월 공고 상세 패키지")
        Mockito.`when`(it.startAt).thenReturn(START)
        Mockito.`when`(it.endAt).thenReturn(START.plusMonths(1))
        Mockito.`when`(it.advertiserId).thenReturn(ADVERTISER_ID)
        Mockito.`when`(it.amount).thenReturn(1_500_000L)
        Mockito.`when`(it.paid).thenReturn(true)
        Mockito.`when`(it.products).thenReturn(mutableListOf(AdContractProduct.BANNER_JOB_DETAIL_SIDE))
    }

    private companion object {
        const val ADVERTISER_ID = 1L
        val FROM: LocalDate = LocalDate.of(2026, 10, 26)
        val TO: LocalDate = LocalDate.of(2026, 12, 7)
        val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 0, 0)
    }
}
