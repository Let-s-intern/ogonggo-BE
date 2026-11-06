package com.ogonggo.core.adposting.implement

import com.ogonggo.core.adposting.domain.AdPosting
import com.ogonggo.core.adposting.implement.dto.AdPostingCountDto
import com.ogonggo.core.adposting.persistence.AdPostingJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/** 광고주·광고 계약마다 광고 공고를 센다. 광고주 광고 수(`AdvertiserAdCountReader`)가 쓴다. */
@Component
class AdPostingCountReader internal constructor(
    private val adPostingRepository: AdPostingJpaRepository,
    private val adPostingTargetReader: AdPostingTargetReader,
) {

    /** 광고 공고가 있는 광고주만 결과에 들어간다. 노출 중은 켜져 있고 기간 안이며 대상이 공개·모집 중인 것이다. */
    fun readByAdvertisers(advertiserIds: Collection<Long>, now: LocalDateTime): Map<Long, AdPostingCountDto> {
        if (advertiserIds.isEmpty()) {
            return emptyMap()
        }
        val adPostings = adPostingRepository.findAllByAdvertiserIdInAndDeletedAtIsNull(advertiserIds)
        val targets = adPostingTargetReader.read(adPostings.filter { it.isLive(now) }, now)
        return adPostings.groupBy(AdPosting::advertiserId).mapValues { (_, owned) ->
            AdPostingCountDto(
                adPostingCount = owned.size,
                activeAdPostingCount = owned.count { targets[it.id]?.available == true },
                latestEndAt = owned.mapNotNull(AdPosting::endAt).maxOrNull(),
            )
        }
    }

    /** 광고 공고가 이어진 계약만 결과에 들어간다. */
    fun readByContracts(contractIds: Collection<Long>): Map<Long, Int> =
        if (contractIds.isEmpty()) {
            emptyMap()
        } else {
            adPostingRepository.findAllByContractIdInAndDeletedAtIsNull(contractIds)
                .groupingBy { checkNotNull(it.contractId) }
                .eachCount()
        }
}
