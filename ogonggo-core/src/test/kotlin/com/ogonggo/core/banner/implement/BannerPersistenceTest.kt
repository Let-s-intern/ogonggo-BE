package com.ogonggo.core.banner.implement

import com.ogonggo.core.adposting.implement.AdPostingCountReader
import com.ogonggo.core.adposting.implement.AdPostingManager
import com.ogonggo.core.adposting.implement.AdPostingTargetReader
import com.ogonggo.core.advertiser.implement.AdvertiserAdCountReader
import com.ogonggo.core.advertisercontract.domain.AdvertiserContractTerms
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractAppender
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractManager
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.advertisercontract.implement.dto.AdvertiserContractAppendDto
import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.core.banner.domain.BannerSettings
import com.ogonggo.core.banner.implement.dto.BannerAppendDto
import com.ogonggo.core.banner.persistence.BannerQueryRepository
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    BannerReader::class,
    BannerAppender::class,
    BannerManager::class,
    BannerQueryRepository::class,
    AdvertiserAdCountReader::class,
    AdvertiserContractReader::class,
    AdvertiserContractAppender::class,
    AdvertiserContractManager::class,
    AdPostingManager::class,
    AdPostingCountReader::class,
    AdPostingTargetReader::class,
)
internal class BannerPersistenceTest @Autowired constructor(
    private val reader: BannerReader,
    private val appender: BannerAppender,
    private val manager: BannerManager,
    private val adCountReader: AdvertiserAdCountReader,
    private val contractReader: AdvertiserContractReader,
    private val contractAppender: AdvertiserContractAppender,
    private val contractManager: AdvertiserContractManager,
    private val entityManager: EntityManager,
) {

    @Test
    fun `등록하면 그 자리 맨 뒤에 붙고 지우면 남은 배너의 순서를 1부터 다시 매긴다`() {
        // given
        val first = append(BannerPlacement.HOME_TOP)
        val second = append(BannerPlacement.HOME_TOP)
        val third = append(BannerPlacement.HOME_TOP)
        val otherPlacement = append(BannerPlacement.HOME_BOTTOM)

        // when
        manager.delete(reader.readForDelete(first), NOW)
        manager.delete(reader.readForDelete(first), NOW.plusDays(1))
        flush()

        // then
        val banners = reader.readAllInPlacement(BannerPlacement.HOME_TOP)
        assertEquals(listOf(second, third), banners.map { it.id })
        assertEquals(listOf(1, 2), banners.map { it.displayOrder })
        assertEquals(1, reader.read(otherPlacement).displayOrder)
        assertEquals(NOW, reader.readForDelete(first).deletedAt)
    }

    @Test
    fun `순서를 통째로 바꾼다`() {
        // given
        val first = append(BannerPlacement.JOB_DETAIL_SIDE)
        val second = append(BannerPlacement.JOB_DETAIL_SIDE)

        // when
        manager.reorder(listOf(reader.read(second), reader.read(first)))
        flush()

        // then
        assertEquals(listOf(second, first), reader.readAllInPlacement(BannerPlacement.JOB_DETAIL_SIDE).map { it.id })
    }

    @Test
    fun `공개 조회는 그 자리에 지금 노출 중인 배너만 순서대로 준다`() {
        // given
        val live = append(BannerPlacement.HOME_TOP, startAt = NOW, endAt = NOW.plusDays(1))
        append(BannerPlacement.HOME_TOP, enabled = false)
        append(BannerPlacement.HOME_TOP, startAt = NOW.plusSeconds(1))
        append(BannerPlacement.HOME_TOP, endAt = NOW)
        val openEnded = append(BannerPlacement.HOME_TOP)
        manager.delete(reader.readForDelete(append(BannerPlacement.HOME_TOP)), NOW)
        append(BannerPlacement.HOME_BOTTOM)
        flush()

        // when
        val banners = reader.readLive(BannerPlacement.HOME_TOP, NOW)

        // then
        assertEquals(listOf(live, openEnded), banners.map { it.id })
    }

    @Test
    fun `광고주의 배너는 자리 선언 순서와 자리 안 순서대로 모으고 광고주와 계약마다 센다`() {
        // given
        val contractId = contract()
        val bootcamp = append(BannerPlacement.BOOTCAMP_TOP, advertiserId = ADVERTISER_ID, contractId = contractId, endAt = NOW.plusDays(30))
        val homeBottom = append(BannerPlacement.HOME_BOTTOM, advertiserId = ADVERTISER_ID, enabled = false, endAt = NOW.plusDays(60))
        val homeTop = append(BannerPlacement.HOME_TOP, advertiserId = ADVERTISER_ID, contractId = contractId)
        manager.delete(reader.readForDelete(append(BannerPlacement.HOME_TOP, advertiserId = ADVERTISER_ID, endAt = NOW.plusDays(90))), NOW)
        append(BannerPlacement.HOME_TOP, advertiserId = ADVERTISER_ID + 1)
        flush()

        // when
        val banners = reader.readAllByAdvertiser(ADVERTISER_ID)
        val counts = adCountReader.readByAdvertisers(listOf(ADVERTISER_ID, ADVERTISER_ID + 2), NOW)
        val contractCounts = adCountReader.readByContracts(listOf(contractId))

        // then
        assertEquals(listOf(homeTop, homeBottom, bootcamp), banners.map { it.id })
        assertEquals(3, counts.getValue(ADVERTISER_ID).bannerCount)
        assertEquals(2, counts.getValue(ADVERTISER_ID).activeBannerCount)
        assertEquals(NOW.plusDays(60), counts.getValue(ADVERTISER_ID).latestEndAt)
        assertEquals(0, counts.getValue(ADVERTISER_ID + 2).bannerCount)
        assertNull(counts.getValue(ADVERTISER_ID + 2).latestEndAt)
        assertEquals(2, contractCounts.getValue(contractId).bannerCount)
    }

    @Test
    fun `계약을 지우면 이어 둔 배너는 남기고 계약 연결만 끊는다`() {
        // given
        val contractId = contract()
        val banner = append(BannerPlacement.HOME_TOP, advertiserId = ADVERTISER_ID, contractId = contractId)

        // when
        contractManager.delete(contractReader.readForDelete(contractId), NOW)
        flush()

        // then
        assertNull(reader.read(banner).contractId)
        assertEquals(ADVERTISER_ID, reader.read(banner).advertiserId)
    }

    private fun append(
        placement: BannerPlacement,
        advertiserId: Long? = null,
        contractId: Long? = null,
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null,
        enabled: Boolean = true,
    ): Long = checkNotNull(
        appender.append(
            BannerAppendDto(
                placement = placement,
                settings = BannerSettings(
                    name = "배너",
                    pcImageUrl = "/banners/jobs-pc.svg",
                    mobileImageUrl = "/banners/jobs-mo.svg",
                    linkUrl = "/calendar",
                    openInNewTab = false,
                    altText = "새 공고",
                    showAdLabel = advertiserId != null,
                    startAt = startAt,
                    endAt = endAt,
                    enabled = enabled,
                    advertiserId = advertiserId,
                    contractId = contractId,
                ),
            ),
        ).id,
    ).also { entityManager.flush() }

    private fun contract(): Long = checkNotNull(
        contractAppender.append(
            AdvertiserContractAppendDto(
                advertiserId = ADVERTISER_ID,
                terms = AdvertiserContractTerms(
                    name = null,
                    startAt = NOW,
                    endAt = NOW.plusMonths(1),
                    amount = 0,
                    vatIncluded = true,
                    paid = false,
                    paidDate = null,
                    products = emptyList(),
                    memo = null,
                ),
            ),
        ).id,
    )

    private fun flush() {
        entityManager.flush()
        entityManager.clear()
    }

    companion object {
        private const val ADVERTISER_ID = 71000L
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 12, 0)
    }
}
