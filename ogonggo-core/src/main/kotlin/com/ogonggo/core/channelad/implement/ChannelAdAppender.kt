package com.ogonggo.core.channelad.implement

import com.ogonggo.core.channelad.domain.ChannelAd
import com.ogonggo.core.channelad.implement.dto.ChannelAdAppendDto
import com.ogonggo.core.channelad.persistence.ChannelAdJpaRepository
import org.springframework.stereotype.Component

@Component
class ChannelAdAppender internal constructor(
    private val channelAdRepository: ChannelAdJpaRepository,
) {

    fun append(dto: ChannelAdAppendDto): ChannelAd = channelAdRepository.save(ChannelAd(dto.terms))
}
