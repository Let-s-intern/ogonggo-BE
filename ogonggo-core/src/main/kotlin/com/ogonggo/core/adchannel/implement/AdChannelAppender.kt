package com.ogonggo.core.adchannel.implement

import com.ogonggo.core.adchannel.domain.AdChannel
import com.ogonggo.core.adchannel.implement.dto.AdChannelAppendDto
import com.ogonggo.core.adchannel.persistence.AdChannelJpaRepository
import org.springframework.stereotype.Component

@Component
class AdChannelAppender internal constructor(
    private val adChannelRepository: AdChannelJpaRepository,
) {

    fun append(dto: AdChannelAppendDto): AdChannel =
        adChannelRepository.save(AdChannel(platform = dto.platform, profile = dto.profile))
}
