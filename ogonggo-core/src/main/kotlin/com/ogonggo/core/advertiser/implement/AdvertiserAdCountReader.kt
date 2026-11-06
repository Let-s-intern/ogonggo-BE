package com.ogonggo.core.advertiser.implement

import com.ogonggo.core.adposting.implement.AdPostingCountReader
import com.ogonggo.core.advertiser.implement.dto.AdvertiserAdCountDto
import com.ogonggo.core.advertiser.implement.dto.AdvertiserContractAdCountDto
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 광고주·광고 계약마다 배너와 광고 공고를 센다. 광고 수를 싣는 응답은 모두 여기서 읽는다.
 *
 * 배너(LC-3576)가 아직 없어 배너 수는 0이다. 광고 공고 수는 [AdPostingCountReader]가 센다.
 */
@Component
class AdvertiserAdCountReader(
    private val adPostingCountReader: AdPostingCountReader,
) {

    /** [now] 기준으로 노출 중인 것을 함께 센다. 모든 광고주 식별자가 결과에 들어간다. */
    fun readByAdvertisers(advertiserIds: Collection<Long>, now: LocalDateTime): Map<Long, AdvertiserAdCountDto> {
        val adPostingCounts = adPostingCountReader.readByAdvertisers(advertiserIds, now)
        return advertiserIds.associateWith { advertiserId ->
            val adPostingCount = adPostingCounts[advertiserId] ?: return@associateWith AdvertiserAdCountDto.NONE
            AdvertiserAdCountDto.NONE.copy(
                adPostingCount = adPostingCount.adPostingCount,
                activeAdPostingCount = adPostingCount.activeAdPostingCount,
                latestEndAt = adPostingCount.latestEndAt,
            )
        }
    }

    /** 모든 계약 식별자가 결과에 들어간다. */
    fun readByContracts(contractIds: Collection<Long>): Map<Long, AdvertiserContractAdCountDto> {
        val adPostingCounts = adPostingCountReader.readByContracts(contractIds)
        return contractIds.associateWith { contractId ->
            AdvertiserContractAdCountDto.NONE.copy(adPostingCount = adPostingCounts[contractId] ?: 0)
        }
    }
}
