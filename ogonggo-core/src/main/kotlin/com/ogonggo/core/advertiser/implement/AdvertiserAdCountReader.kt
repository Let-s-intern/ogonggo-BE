package com.ogonggo.core.advertiser.implement

import com.ogonggo.core.advertiser.implement.dto.AdvertiserAdCountDto
import com.ogonggo.core.advertiser.implement.dto.AdvertiserContractAdCountDto
import com.ogonggo.core.banner.implement.BannerReader
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 광고주·광고 계약마다 배너와 광고 공고를 센다. 광고 수를 싣는 응답은 모두 여기서 읽는다.
 *
 * 배너는 광고주·계약 식별자로 묶어 센다(지운 배너는 뺀다). 광고 공고(LC-3577)는 아직 없어 0이다.
 */
@Component
class AdvertiserAdCountReader(
    private val bannerReader: BannerReader,
) {

    /** [now] 기준으로 노출 중인 것을 함께 센다. 모든 광고주 식별자가 결과에 들어간다. */
    fun readByAdvertisers(advertiserIds: Collection<Long>, now: LocalDateTime): Map<Long, AdvertiserAdCountDto> {
        val banners = bannerReader.readAllByAdvertisers(advertiserIds).groupBy { it.advertiserId }
        return advertiserIds.associateWith { advertiserId ->
            val advertiserBanners = banners[advertiserId].orEmpty()
            AdvertiserAdCountDto.NONE.copy(
                bannerCount = advertiserBanners.size,
                activeBannerCount = advertiserBanners.count { it.isLive(now) },
                latestEndAt = advertiserBanners.mapNotNull { it.endAt }.maxOrNull(),
            )
        }
    }

    /** 모든 계약 식별자가 결과에 들어간다. */
    fun readByContracts(contractIds: Collection<Long>): Map<Long, AdvertiserContractAdCountDto> {
        val bannerCounts = bannerReader.readAllByContracts(contractIds).groupingBy { it.contractId }.eachCount()
        return contractIds.associateWith { AdvertiserContractAdCountDto.NONE.copy(bannerCount = bannerCounts[it] ?: 0) }
    }
}
