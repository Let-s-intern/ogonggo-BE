package com.ogonggo.core.advertisercontract.implement

import com.ogonggo.core.advertisercontract.domain.AdvertiserContract
import com.ogonggo.core.advertisercontract.implement.dto.AdvertiserContractAppendDto
import com.ogonggo.core.advertisercontract.persistence.AdvertiserContractJpaRepository
import org.springframework.stereotype.Component

@Component
class AdvertiserContractAppender internal constructor(
    private val advertiserContractRepository: AdvertiserContractJpaRepository,
) {

    fun append(dto: AdvertiserContractAppendDto): AdvertiserContract =
        advertiserContractRepository.save(AdvertiserContract(advertiserId = dto.advertiserId, terms = dto.terms))
}
