package com.ogonggo.core.channelad.implement

import com.ogonggo.core.channelad.domain.ChannelAd
import com.ogonggo.core.channelad.domain.ChannelAdTerms
import com.ogonggo.core.channelad.persistence.ChannelAdJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ChannelAdManager internal constructor(
    private val channelAdRepository: ChannelAdJpaRepository,
) {

    fun change(ad: ChannelAd, terms: ChannelAdTerms) {
        ad.change(terms)
        channelAdRepository.save(ad)
    }

    fun delete(ad: ChannelAd, now: LocalDateTime) {
        ad.delete(now)
        channelAdRepository.save(ad)
    }
}
