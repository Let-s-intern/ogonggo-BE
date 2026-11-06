package com.ogonggo.core.advertisercontract.implement

import com.ogonggo.core.adposting.implement.AdPostingManager
import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.domain.AdvertiserContractTerms
import com.ogonggo.core.advertisercontract.persistence.AdvertiserContractJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdvertiserContractManager internal constructor(
    private val advertiserContractRepository: AdvertiserContractJpaRepository,
    private val adPostingManager: AdPostingManager,
) {

    fun change(contract: AdvertiserContract, terms: AdvertiserContractTerms) {
        contract.change(terms)
        advertiserContractRepository.save(contract)
    }

    /**
     * 계약만 지운다. 이어 둔 배너·광고 공고는 지우지 않고 잇는 값만 끊어야 한다. 배너(LC-3576)가
     * 계약을 잇게 되면 여기서 함께 끊는다.
     */
    fun delete(contract: AdvertiserContract, now: LocalDateTime) {
        contract.delete(now)
        advertiserContractRepository.save(contract)
        adPostingManager.unlinkContract(checkNotNull(contract.id) { "광고 계약 식별자가 없습니다." })
    }
}
