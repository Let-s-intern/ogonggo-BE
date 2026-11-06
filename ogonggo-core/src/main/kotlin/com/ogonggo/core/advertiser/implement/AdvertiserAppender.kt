package com.ogonggo.core.advertiser.implement

import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.implement.dto.AdvertiserAppendDto
import com.ogonggo.core.advertiser.persistence.AdvertiserJpaRepository
import org.springframework.stereotype.Component

@Component
class AdvertiserAppender internal constructor(
    private val advertiserRepository: AdvertiserJpaRepository,
) {

    /** 운영자가 직접 더하는 비회원 광고주다. */
    fun appendManual(dto: AdvertiserAppendDto): Advertiser =
        advertiserRepository.save(Advertiser.manual(name = dto.name, contact = dto.contact))
}
