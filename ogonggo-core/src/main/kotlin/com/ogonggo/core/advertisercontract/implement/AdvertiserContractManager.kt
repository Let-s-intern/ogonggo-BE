package com.ogonggo.core.advertisercontract.implement

import com.ogonggo.core.adposting.implement.AdPostingManager
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.domain.AdvertiserContractTerms
import com.ogonggo.core.advertisercontract.persistence.AdvertiserContractJpaRepository
import com.ogonggo.core.banner.implement.BannerManager
import com.ogonggo.core.channelad.persistence.ChannelAdJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdvertiserContractManager internal constructor(
    private val advertiserContractRepository: AdvertiserContractJpaRepository,
    private val adPostingManager: AdPostingManager,
    private val bannerManager: BannerManager,
    private val channelAdRepository: ChannelAdJpaRepository,
) {

    fun change(contract: AdvertiserContract, terms: AdvertiserContractTerms) {
        contract.change(terms)
        advertiserContractRepository.save(contract)
    }

    /**
     * 계약만 지운다. 이어 둔 배너·광고 공고·채널 광고는 지우지 않고 잇는 값만 끊는다.
     */
    fun delete(contract: AdvertiserContract, now: LocalDateTime) {
        contract.delete(now)
        advertiserContractRepository.save(contract)
        bannerManager.unlinkContract(checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." })
        adPostingManager.unlinkContract(checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." })
        channelAdRepository.unlinkContract(checkNotNull(contract.id), now)
    }

    /** 지운 채널 게재 유형을 그 유형을 산 계약에서 뺀다. */
    fun removeChannelFormat(formatId: Long) {
        val contracts = advertiserContractRepository.findAllByChannelFormatId(formatId)
        contracts.forEach { it.removeChannelFormat(formatId) }
        advertiserContractRepository.saveAll(contracts)
    }
}
