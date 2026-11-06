package com.ogonggo.core.advertisercontract.implement.dto

import com.ogonggo.core.advertisercontract.domain.AdvertiserContractTerms

data class AdvertiserContractAppendDto(
    val advertiserId: Long,
    val terms: AdvertiserContractTerms,
)
