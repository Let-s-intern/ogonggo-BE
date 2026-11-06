package com.ogonggo.core.advertisercontract.implement

import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.domain.AdvertiserContractTerms
import com.ogonggo.core.advertisercontract.persistence.AdvertiserContractJpaRepository
import com.ogonggo.core.channelad.persistence.ChannelAdJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdvertiserContractManager internal constructor(
    private val advertiserContractRepository: AdvertiserContractJpaRepository,
    private val channelAdRepository: ChannelAdJpaRepository,
) {

    fun change(contract: AdvertiserContract, terms: AdvertiserContractTerms) {
        contract.change(terms)
        advertiserContractRepository.save(contract)
    }

    /**
     * 계약만 지운다. 이어 둔 배너·광고 공고는 지우지 않고 잇는 값만 끊어야 한다. 배너(LC-3576)·광고 공고(LC-3577)가
     * 계약을 잇게 되면 여기서 함께 끊는다.
     */
    fun delete(contract: AdvertiserContract, now: LocalDateTime) {
        contract.delete(now)
        advertiserContractRepository.save(contract)
        channelAdRepository.unlinkContract(checkNotNull(contract.id), now)
    }

    /** 지운 채널 게재 유형을 그 유형을 산 계약에서 뺀다. */
    fun removeChannelFormat(formatId: Long) {
        val contracts = advertiserContractRepository.findAllByChannelFormatId(formatId)
        contracts.forEach { it.removeChannelFormat(formatId) }
        advertiserContractRepository.saveAll(contracts)
    }
}
