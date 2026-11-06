package com.ogonggo.core.channelad.implement

import com.ogonggo.core.adchannel.domain.AdChannelPlatform
import com.ogonggo.core.advertisercontract.domain.AdvertiserContractTerms
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractAppender
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractManager
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.advertisercontract.implement.dto.AdvertiserContractAppendDto
import com.ogonggo.core.banner.implement.BannerManager
import com.ogonggo.core.channelad.domain.ChannelAdSearchCondition
import com.ogonggo.core.channelad.domain.ChannelAdStatus
import com.ogonggo.core.channelad.domain.ChannelAdTerms
import com.ogonggo.core.channelad.implement.dto.ChannelAdAppendDto
import com.ogonggo.core.channelad.persistence.ChannelAdQueryRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    ChannelAdReader::class,
    ChannelAdQueryRepository::class,
    ChannelAdAppender::class,
    ChannelAdManager::class,
    AdvertiserContractReader::class,
    AdvertiserContractAppender::class,
    AdvertiserContractManager::class,
    BannerManager::class,
)
internal class ChannelAdPersistenceTest @Autowired constructor(
    private val reader: ChannelAdReader,
    private val appender: ChannelAdAppender,
    private val manager: ChannelAdManager,
    private val contractReader: AdvertiserContractReader,
    private val contractAppender: AdvertiserContractAppender,
    private val contractManager: AdvertiserContractManager,
    private val entityManager: EntityManager,
) {

    @Test
    fun `달로 거르면 게시일부터 노출 끝 날까지가 그달에 걸친 광고를 게시일, 식별자 순으로 준다`() {
        // given
        val octoberThrough = append(postDate = LocalDate.of(2026, 10, 25), endDate = LocalDate.of(2026, 11, 1))
        val novemberLast = append(postDate = LocalDate.of(2026, 11, 30))
        val novemberFirstA = append(postDate = LocalDate.of(2026, 11, 1))
        val novemberFirstB = append(postDate = LocalDate.of(2026, 11, 1))
        append(postDate = LocalDate.of(2026, 10, 20), endDate = LocalDate.of(2026, 10, 31))
        append(postDate = LocalDate.of(2026, 12, 1))
        val deleted = append(postDate = LocalDate.of(2026, 11, 2))
        manager.delete(reader.readForDelete(deleted), NOW)
        entityManager.flush()

        // when
        val ads = reader.readAll(ChannelAdSearchCondition(from = LocalDate.of(2026, 11, 1), toExclusive = LocalDate.of(2026, 12, 1)))

        // then
        assertEquals(listOf(octoberThrough, novemberFirstA, novemberFirstB, novemberLast), ads.map { it.id })
    }

    @Test
    fun `광고주·계약·플랫폼·채널·유형·상태로 거른다`() {
        // given
        val target = append(advertiserId = 2, contractId = 7, platform = AdChannelPlatform.KAKAO_OPENCHAT, channelId = 6, formatId = 8, status = ChannelAdStatus.PUBLISHED)
        append(advertiserId = 2, contractId = 7, platform = AdChannelPlatform.KAKAO_OPENCHAT, channelId = 6, formatId = 8)
        append()

        // when
        val ads = reader.readAll(
            ChannelAdSearchCondition(
                advertiserId = 2,
                contractId = 7,
                platform = AdChannelPlatform.KAKAO_OPENCHAT,
                channelId = 6,
                formatId = 8,
                status = ChannelAdStatus.PUBLISHED,
            ),
        )

        // then
        assertEquals(listOf(target), ads.map { it.id })
    }

    @Test
    fun `채널·유형·계약마다 지우지 않은 광고를 세고 없는 것은 0이다`() {
        // given
        append(channelId = 1, formatId = 1, contractId = 3)
        append(channelId = 1, formatId = 2, contractId = 3)
        val deleted = append(channelId = 2, formatId = 2)
        manager.delete(reader.readForDelete(deleted), NOW)
        entityManager.flush()

        // then
        assertEquals(mapOf(1L to 2, 2L to 0), reader.countByChannels(listOf(1, 2)))
        assertEquals(mapOf(1L to 1, 2L to 1), reader.countByFormats(listOf(1, 2)))
        assertEquals(mapOf(3L to 2, 4L to 0), reader.countByContracts(listOf(3, 4)))
        assertTrue(reader.existsByChannel(1))
        assertFalse(reader.existsByChannel(2))
        assertTrue(reader.existsByFormat(2))
    }

    @Test
    fun `계약을 지우면 이어 둔 채널 광고는 남기고 계약 연결만 끊는다`() {
        // given
        val contract = contractAppender.append(AdvertiserContractAppendDto(ADVERTISER_ID, contractTerms(channelFormatIds = listOf(3, 1))))
        val contractId = checkNotNull(contract.id)
        val ad = append(contractId = contractId)
        entityManager.flush()
        entityManager.clear()

        // when
        contractManager.delete(contractReader.readForDelete(contractId), NOW)
        entityManager.flush()
        entityManager.clear()

        // then
        val unlinked = reader.read(ad)
        assertNull(unlinked.contractId)
        assertEquals(NOW, unlinked.updatedAt)
    }

    @Test
    fun `계약은 산 채널 게재 유형을 저장하고 지운 유형을 그 유형을 산 계약에서 뺀다`() {
        // given
        val contract = contractAppender.append(AdvertiserContractAppendDto(ADVERTISER_ID, contractTerms(channelFormatIds = listOf(3, 1, 3))))
        val contractId = checkNotNull(contract.id)
        entityManager.flush()
        entityManager.clear()
        assertEquals(setOf(1L, 3L), contractReader.read(contractId).channelFormatIds.toSet())

        // when
        contractManager.removeChannelFormat(3)
        entityManager.flush()
        entityManager.clear()

        // then
        assertEquals(listOf(1L), contractReader.read(contractId).channelFormatIds)
    }

    private fun append(
        advertiserId: Long = ADVERTISER_ID,
        contractId: Long? = null,
        platform: AdChannelPlatform = AdChannelPlatform.INSTAGRAM,
        channelId: Long = 1,
        formatId: Long = 1,
        postDate: LocalDate = LocalDate.of(2026, 11, 10),
        endDate: LocalDate? = null,
        status: ChannelAdStatus = ChannelAdStatus.SCHEDULED,
    ): Long {
        val ad = appender.append(
            ChannelAdAppendDto(
                ChannelAdTerms(
                    advertiserId = advertiserId,
                    contractId = contractId,
                    platform = platform,
                    channelId = channelId,
                    formatId = formatId,
                    postDate = postDate,
                    endDate = endDate,
                    status = status,
                    postUrl = null,
                    creativeMemo = null,
                    adminMemo = null,
                ),
            ),
        )
        entityManager.flush()
        return checkNotNull(ad.id)
    }

    private fun contractTerms(channelFormatIds: List<Long>) = AdvertiserContractTerms(
        name = null,
        startAt = LocalDateTime.of(2026, 11, 1, 0, 0),
        endAt = LocalDateTime.of(2026, 12, 1, 0, 0),
        amount = 1_000_000,
        vatIncluded = true,
        paid = false,
        paidDate = null,
        products = emptyList(),
        memo = null,
        channelFormatIds = channelFormatIds,
    )

    private companion object {
        const val ADVERTISER_ID = 1L
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
