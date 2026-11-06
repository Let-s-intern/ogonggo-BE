package com.ogonggo.core.adchannel.implement

import com.ogonggo.core.adchannel.domain.AdChannel
import com.ogonggo.core.adchannel.domain.AdChannelProfile
import com.ogonggo.core.adchannel.persistence.AdChannelJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdChannelManager internal constructor(
    private val adChannelRepository: AdChannelJpaRepository,
) {

    fun change(channel: AdChannel, profile: AdChannelProfile) {
        channel.change(profile)
        adChannelRepository.save(channel)
    }

    /** 채널만 지운다. 게재 유형의 올릴 수 있는 채널에서 빼는 일은 게재 유형 쪽이 한다. */
    fun delete(channel: AdChannel, now: LocalDateTime) {
        channel.delete(now)
        adChannelRepository.save(channel)
    }
}
