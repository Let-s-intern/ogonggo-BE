package com.ogonggo.adminapi.channelad.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.adchannel.domain.AdChannel
import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.adchannel.implement.AdChannelReader
import com.ogonggo.core.adchannelformat.domain.AdChannelFormat
import com.ogonggo.core.adchannelformat.implement.AdChannelFormatReader
import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.channelad.domain.ChannelAd
import com.ogonggo.core.channelad.domain.ChannelAdStatus
import com.ogonggo.core.channelad.implement.ChannelAdAppender
import com.ogonggo.core.channelad.implement.ChannelAdManager
import com.ogonggo.core.channelad.implement.ChannelAdReader
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.LocalDate
import java.util.Optional

class AdminChannelAdServiceTest {

    private val channelAdReader = Mockito.mock(ChannelAdReader::class.java)
    private val channelAdAppender = Mockito.mock(ChannelAdAppender::class.java)
    private val channelAdManager = Mockito.mock(ChannelAdManager::class.java)
    private val advertiserReader = Mockito.mock(AdvertiserReader::class.java)
    private val advertiserContractReader = Mockito.mock(AdvertiserContractReader::class.java)
    private val adChannelReader = Mockito.mock(AdChannelReader::class.java)
    private val adChannelFormatReader = Mockito.mock(AdChannelFormatReader::class.java)
    private val service = AdminChannelAdService(
        channelAdReader = channelAdReader,
        channelAdAppender = channelAdAppender,
        channelAdManager = channelAdManager,
        advertiserReader = advertiserReader,
        advertiserContractReader = advertiserContractReader,
        adChannelReader = adChannelReader,
        adChannelFormatReader = adChannelFormatReader,
        clock = Clock.systemDefaultZone(),
    )

    @BeforeEach
    fun setUp() {
        Mockito.`when`(advertiserReader.readAllByIds(listOf(ADVERTISER_ID))).thenReturn(mapOf(ADVERTISER_ID to Mockito.mock(Advertiser::class.java)))
        val contract = Mockito.mock(AdvertiserContract::class.java)
        Mockito.`when`(contract.advertiserId).thenReturn(OTHER_ADVERTISER_ID)
        Mockito.`when`(advertiserContractReader.readAllByIds(listOf(OTHER_CONTRACT_ID))).thenReturn(mapOf(OTHER_CONTRACT_ID to contract))
    }

    @Test
    fun `광고주·계약·채널·유형을 목과 같은 순서로 보고 처음 걸린 칸 하나만 알린다`() {
        stubChannel(id = 1, platform = AdChannelPlatform.INSTAGRAM, handle = "letscareer.job")
        stubChannel(id = 2, platform = AdChannelPlatform.INSTAGRAM, handle = "letscareer.univ", enabled = false)
        stubChannel(id = 3, platform = AdChannelPlatform.INSTAGRAM, handle = "letscareer.qna")
        stubChannel(id = 6, platform = AdChannelPlatform.KAKAO_OPENCHAT, handle = null)
        stubFormat(id = 1, platform = AdChannelPlatform.INSTAGRAM, name = "피드 게시물")
        stubFormat(id = 5, platform = AdChannelPlatform.INSTAGRAM, name = "캐러셀", channelIds = listOf(1, 2, 5))
        stubFormat(id = 3, platform = AdChannelPlatform.INSTAGRAM, name = "스토리", enabled = false)

        assertMessage("advertiserId: 있는 광고주를 골라 주세요.", create(advertiserId = 99, channelId = 99))
        assertMessage("contractId: 있는 계약을 골라 주세요.", create(contractId = 98))
        assertMessage("contractId: 고른 광고주의 계약만 이을 수 있습니다.", create(contractId = OTHER_CONTRACT_ID))
        assertMessage("channelId: 있는 채널을 골라 주세요.", create(channelId = 99))
        assertMessage("channelId: 사용 안 함인 채널은 고를 수 없습니다.", create(channelId = 2))
        assertMessage("formatId: 있는 게재 유형을 골라 주세요.", create(formatId = 99))
        assertMessage("formatId: 사용 안 함인 게재 유형은 고를 수 없습니다.", create(formatId = 3))
        assertMessage("formatId: 채널과 같은 플랫폼의 게재 유형을 골라 주세요.", create(channelId = 6, formatId = 1))
        assertMessage("formatId: @letscareer.qna 에는 캐러셀 을(를) 올리지 않습니다.", create(channelId = 3, formatId = 5))
    }

    @Test
    fun `끈 채널·유형도 이미 그 값으로 잡힌 광고는 그대로 고칠 수 있다`() {
        // given
        stubChannel(id = 2, platform = AdChannelPlatform.INSTAGRAM, handle = "letscareer.univ", enabled = false)
        stubFormat(id = 3, platform = AdChannelPlatform.INSTAGRAM, name = "스토리", enabled = false)
        val stored = Mockito.mock(ChannelAd::class.java)
        Mockito.`when`(stored.advertiserId).thenReturn(ADVERTISER_ID)
        Mockito.`when`(stored.contractId).thenReturn(null)
        Mockito.`when`(stored.channelId).thenReturn(2)
        Mockito.`when`(stored.formatId).thenReturn(3)
        Mockito.`when`(stored.postDate).thenReturn(LocalDate.of(2026, 11, 10))
        Mockito.`when`(stored.status).thenReturn(ChannelAdStatus.SCHEDULED)
        Mockito.`when`(channelAdReader.readForUpdate(AD_ID)).thenReturn(stored)

        // when & then
        assertDoesNotThrow { service.updateChannelAd(AD_ID, AdminChannelAdUpdateCommand(creativeMemo = Optional.of("문구"))) }
        assertMessage("channelId: 사용 안 함인 채널은 고를 수 없습니다.", create(channelId = 2, formatId = 3))
    }

    private fun assertMessage(message: String, command: AdminChannelAdCreateCommand) {
        val exception = assertThrows(AdminConsoleBadRequestException::class.java) { service.createChannelAd(command) }
        assertEquals(message, exception.message)
    }

    private fun stubChannel(id: Long, platform: AdChannelPlatform, handle: String?, enabled: Boolean = true) {
        val channel = Mockito.mock(AdChannel::class.java)
        Mockito.`when`(channel.id).thenReturn(id)
        Mockito.`when`(channel.platform).thenReturn(platform)
        Mockito.`when`(channel.enabled).thenReturn(enabled)
        Mockito.`when`(channel.label).thenReturn(handle?.let { "@$it" } ?: "오픈채팅방")
        Mockito.`when`(adChannelReader.readOrNull(id)).thenReturn(channel)
    }

    private fun stubFormat(id: Long, platform: AdChannelPlatform, name: String, enabled: Boolean = true, channelIds: List<Long> = emptyList()) {
        val format = Mockito.mock(AdChannelFormat::class.java)
        Mockito.`when`(format.id).thenReturn(id)
        Mockito.`when`(format.platform).thenReturn(platform)
        Mockito.`when`(format.name).thenReturn(name)
        Mockito.`when`(format.enabled).thenReturn(enabled)
        Mockito.`when`(format.allows(Mockito.anyLong())).thenAnswer { channelIds.isEmpty() || it.getArgument<Long>(0) in channelIds }
        Mockito.`when`(adChannelFormatReader.readOrNull(id)).thenReturn(format)
    }

    private fun create(advertiserId: Long = ADVERTISER_ID, contractId: Long? = null, channelId: Long = 1, formatId: Long = 5) =
        AdminChannelAdCreateCommand(
            advertiserId = advertiserId,
            contractId = contractId,
            channelId = channelId,
            formatId = formatId,
            postDate = LocalDate.of(2026, 11, 10),
            endDate = null,
            status = null,
            postUrl = null,
            creativeMemo = null,
            adminMemo = null,
        )

    private companion object {
        const val ADVERTISER_ID = 1L
        const val OTHER_ADVERTISER_ID = 2L
        const val OTHER_CONTRACT_ID = 30L
        const val AD_ID = 40L
    }
}
